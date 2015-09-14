/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.kademlia;

import com.offbynull.voip.kademlia.RouteTreeBucketSpecificationSupplier.BucketParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class RouteTree {
    private final Node baseNode;
    private final RouteTreeLevel root;
    private final TimeSet<BitString> bucketUpdateTimes;
    
    private Instant lastUpdateTime;
    
    public RouteTree(Node baseNode, // because id's are always > 0 in size -- it isn't possible for tree creation to mess up
            RouteTreeBranchSpecificationSupplier branchSpecSupplier,
            RouteTreeBucketSpecificationSupplier bucketSpecSupplier) {
        Validate.notNull(baseNode);
        Validate.notNull(branchSpecSupplier);
        Validate.notNull(bucketSpecSupplier);
        
        this.baseNode = baseNode; // must be set before creating RouteTreeLevels
        this.bucketUpdateTimes = new TimeSet<>();

        root = createRoot(branchSpecSupplier, bucketSpecSupplier);
        RouteTreeLevel child = root;
        while (child != null) {
            child = growParent(child, branchSpecSupplier, bucketSpecSupplier);
        }
        
        this.lastUpdateTime = Instant.MIN;
    }

    public List<Activity> findStrict(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(!id.equals(baseNode.getId()));
        Validate.isTrue(id.getBitLength() == baseNode.getId().getBitLength());
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyways

        LinkedHashSet<Activity> output = new LinkedHashSet<>(max); // LinkedHashSet because maintains order and keeps out duplicates
        
        // Go through all buckets with a matching prefix, starting from the largest matching prefix, and grab the 'closest' nodes to an ID.
        // See IdClosenessComparator for description on how closeness is calculated.
        root.findClosestNodesByLargestSharedPrefix(id, output, max);
        if (output.size() >= max) { // should never extend past max, but just incase
            return new ArrayList<>(output);
        }
        
        // At this point, output will contain the closest nodes that share a prefix. However, if we still have room, re-do the search as if
        // all the buckets accessed were empty -- flipping the bits corresponding to those empty buckets, as per Kademlia's notion of
        // closeness. Why do you want to do this? A few reasons...
        //
        // 1. Highly unbalanced networks
        // 2. Networks in their infancy (very few nodes in the network)
        //
        // Assume a 3-bit system with k = 1. Assume that all the 1xxb nodes are in the network, and no 0xxb nodes are present.
        // 
        //         0/ \1
        //         /   \
        //        /     \
        //       /       \
        //      /         \
        //    0/ \1     0/ \1
        //    /   \     /   \ 
        //  0/\1 0/\1 0/\1 0/\1
        //            * *  * *
        //
        // This is a highly unbalanced network
        //
        // Node 111b wants to store a value at 000b, but node 111b has nothing in its 0xxb bucket. The store could be important, so we
        // follow this notion of closeness and flip the top bit, turning 000b to 100b. 100b then becomes the closest node. If it turns out
        // that 100b isn't present in the network as well, we continually flip the next bit until we find a node that is (so if we were to
        // flip again it would be 110b).
        //
        // How likely is this to happen in a real Kademlia network with a key size of 160-bits? Even if the RNG for node ID generation is
        // biased, it probably won't be biased to the point where we have a massively lop-sided tree. But, networks just starting out with
        // only a few nodes in them may encounter this scenario.


        // Get longest common prefix from the currently found closest node (nodes that have a shared prefix in the routing table). If it's
        // self, get next longest prefix.
        int longestPrefixBitLen = output.stream()
                .map(x -> id.getSharedPrefixLength(x.getNode().getId())) // map found id to shared prefix length
                .filter(x -> x < id.getBitLength()) // throw out if self (if longest common prefix spans the entire id)
                .max(Integer::compare) // get largest prefix
                .orElseGet(() -> 0); // no largest prefix, return 0
        // Start flipping bits at just after the largest prefix found in search aboveStart until we have an output that reaches max or we've
        // exhausted the remaining bits we could flip.
        int bitOffset = 0;
        int bitLen = longestPrefixBitLen;
        Id flippedId = id;
        for (int i = bitLen - 1; i >= 0; i--) {
            flippedId = flippedId.flipBit(i);
            root.findClosestNodesByLargestSharedPrefix(flippedId, output, max);
            if (output.size() >= max) { // should never extend past max, but just incase
                return new ArrayList<>(output);
            }
        }
        
        return new ArrayList<>(output);
    }

    public List<Activity> dumpBucket(BitString prefix) {
        Validate.notNull(prefix);
        Validate.isTrue(prefix.getBitLength() < baseNode.getId().getBitLength()); // cannot be == or >

        LinkedList<Activity> output = new LinkedList<>();
        root.dumpNodesInBucket(prefix, output);
        return new ArrayList<>(output);
    }

    public RouteTreeChangeSet touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        Validate.isTrue(!id.equals(baseNode.getId()));
        Validate.isTrue(id.getBitLength() == baseNode.getId().getBitLength());
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime

        return root.touch(time, node);
    }

    public RouteTreeChangeSet stale(Node node) throws LinkConflictException {
        Validate.notNull(node);

        Id id = node.getId();
        Validate.isTrue(!id.equals(baseNode.getId()));
        Validate.isTrue(id.getBitLength() == baseNode.getId().getBitLength());
            
        return root.stale(node);
    }

    public List<BitString> getBucketsUpdatedBefore(Instant time) {
        return bucketUpdateTimes.getBefore(time, true);
    }
    
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    private static final BitString EMPTY = BitString.createFromString("");

    private RouteTreeLevel createRoot(
            RouteTreeBranchSpecificationSupplier branchSpecSupplier,
            RouteTreeBucketSpecificationSupplier bucketSpecSupplier) {
        Validate.notNull(branchSpecSupplier);
        Validate.notNull(bucketSpecSupplier);


        // Get number of branches/buckets to create for root
        int numOfBuckets = branchSpecSupplier.getBranchCount(EMPTY);
        Validate.isTrue(numOfBuckets >= 0, "Branch cannot be negative, was %d", numOfBuckets);
        Validate.isTrue(numOfBuckets != 0, "Root of tree must contain at least 1 branch, was %d", numOfBuckets);
        Validate.isTrue(Integer.bitCount(numOfBuckets) == 1, "Branch count must be power of 2");
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3
        Validate.isTrue(suffixBitCount <= baseNode.getId().getBitLength(),
                "Attempting to branch too far (in root) %d bits extends past %d bits", suffixBitCount, baseNode.getId().getBitLength());

        
        // Create buckets by creating a 0-sized top bucket and splitting it + resizing each split
        KBucket[] newBuckets = new KBucket(baseNode.getId(), EMPTY, 0, 0).split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = bucketSpecSupplier.getBucketParameters(newBuckets[i].getPrefix());
            int bucketSize = bucketParams.getBucketSize();
            int cacheSize = bucketParams.getCacheSize();
            newBuckets[i].resizeBucket(bucketSize);
            newBuckets[i].resizeCache(cacheSize);
            
            bucketUpdateTimes.insert(newBuckets[i].getLastUpdateTime(), newBuckets[i].getPrefix());
        }

        // Create root
        return new RouteTreeLevel(EMPTY, suffixBitCount, new ArrayList<>(Arrays.asList(newBuckets)));
    }

    private RouteTreeLevel growParent(RouteTreeLevel parent,
            RouteTreeBranchSpecificationSupplier branchSpecSupplier,
            RouteTreeBucketSpecificationSupplier bucketSpecSupplier) {
        Validate.notNull(parent);
        Validate.notNull(branchSpecSupplier);
        Validate.notNull(bucketSpecSupplier);

        
        // Calculate which bucket from parent to split
        int parentNumOfBuckets = parent.kBuckets.size();
        Validate.validState(Integer.bitCount(parentNumOfBuckets) == 1); // sanity check numofbuckets is pow of 2
        int parentPrefixBitLen = parent.prefix.getBitLength(); // num of bits in parent's prefix
        int parentSuffixBitCount = Integer.bitCount(parentNumOfBuckets - 1); // num of bits in parent's suffix
                                                                             // e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3
        
        if (parentPrefixBitLen + parentSuffixBitCount >= baseNode.getId().getBitLength()) { // should never be >, only ==, but just in case
            // The parents prefix length + the number of bits the parent used for buckets > baseId's length. As such, it isn't possible to
            // grow any further, so don't even try.
            return null;
        }
        
        int splitBucketIdx = (int) baseNode.getId().getBitString().getBitsAsLong(parentPrefixBitLen, parentSuffixBitCount);
        BitString splitBucketPrefix = parent.kBuckets.get(splitBucketIdx).getPrefix();
        
        
        // Get number of buckets to create for new level
        int numOfBuckets = branchSpecSupplier.getBranchCount(splitBucketPrefix);
        Validate.isTrue(numOfBuckets >= 0, "Branch cannot be negative, was %d", numOfBuckets);
        if (numOfBuckets == 0) {
            return null; // 0 means do not grow
        }
        Validate.isTrue(Integer.bitCount(numOfBuckets) == 1, "Branch count must be power of 2");
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 (1000) -- 1000 - 1 = 0111, bitcount(0111) = 3
        Validate.isTrue(splitBucketPrefix.getBitLength() + suffixBitCount <= baseNode.getId().getBitLength(),
                "Attempting to branch too far %s with %d bits extends past %d bits", splitBucketPrefix, suffixBitCount,
                baseNode.getId().getBitLength());
        
        
        // Split parent bucket at that branch index
        BitString newPrefix = baseNode.getId().getBitString().getBits(0, parentPrefixBitLen + suffixBitCount);
        KBucket[] newBuckets = parent.kBuckets.get(splitBucketIdx).split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = bucketSpecSupplier.getBucketParameters(newBuckets[i].getPrefix());
            int bucketSize = bucketParams.getBucketSize();
            int cacheSize = bucketParams.getCacheSize();
            newBuckets[i].resizeBucket(bucketSize);
            newBuckets[i].resizeCache(cacheSize);
            
            bucketUpdateTimes.insert(newBuckets[i].getLastUpdateTime(), newBuckets[i].getPrefix());
        }

        // Get rid of parent bucket we just split. It branches down at that point, and any nodes that were contained within will be in the
        // newly created buckets
        bucketUpdateTimes.remove(parent.kBuckets.get(splitBucketIdx).getPrefix());
        parent.kBuckets.set(splitBucketIdx, null);

        // Create new level and set as child
        RouteTreeLevel ret = new RouteTreeLevel(newPrefix, suffixBitCount, new ArrayList<>(Arrays.asList(newBuckets)));
        parent.child = ret;
        
        return ret;
    }
    
    private final class RouteTreeLevel {
        private final BitString prefix;
        private final int suffixLen;

        private final List<KBucket> kBuckets;

        private RouteTreeLevel child;

        private RouteTreeLevel(BitString prefix, int suffixLen, List<KBucket> kBuckets) {
            this.prefix = prefix;
            this.suffixLen = suffixLen;
            this.kBuckets = kBuckets;
        }

        public void dumpNodesInBucket(BitString searchPrefix, LinkedList<Activity> output) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(searchPrefix.getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix of searchPrefix matches

            int bucketIdx = (int) searchPrefix.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket targetKBucket = kBuckets.get(bucketIdx);

            if (targetKBucket == null) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we can
                // continue down child to find more buckets with a larger prefix.
                Validate.validState(child != null); // sanity check
                child.dumpNodesInBucket(searchPrefix, output);
            } else {
                // Target kBucket found -- we've reached the end of how far we can go down the routing tree
                output.addAll(targetKBucket.dumpBucket());
            }
        }

        public void findClosestNodesByLargestSharedPrefix(Id id, LinkedHashSet<Activity> output, int max) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket targetKBucket = kBuckets.get(bucketIdx);

            if (targetKBucket == null) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we can
                // continue down child to find more buckets with a larger prefix.
                Validate.validState(child != null); // sanity check
                child.findClosestNodesByLargestSharedPrefix(id, output, max);

                // If we have more room available, scan buckets at this level and add nodes closest to id in to output
                dumpCloseNodesFromKBucketsOnThisLevel(id, output, max);
            } else {
                // Target kBucket found -- we've reached the end of how far we can go down the routing tree
                // Scan buckets at this level and add nodes closes to id in to output
                dumpCloseNodesFromKBucketsOnThisLevel(id, output, max);
            }
        }

        private void dumpCloseNodesFromKBucketsOnThisLevel(Id id, LinkedHashSet<Activity> output, int max) {
            int remaining = max - output.size();
            if (remaining <= 0) {
                return;
            }

            IdClosenessComparator comparator = new IdClosenessComparator(id);
            kBuckets.stream()
                    .filter(x -> x != null) // skip null buckets -- if a bucket is null, the tree branches down at that prefix
                    .flatMap(x -> x.dumpBucket().stream()) // kbucket to nodes in kbuckets
                    .sorted((x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()))
                    .limit(remaining) // limit to amount of nodes we need to insert
                    .forEachOrdered(output::add); // add to output set
        }

        public RouteTreeChangeSet touch(Instant time, Node node) throws LinkConflictException {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Id id = node.getId();
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket bucket = kBuckets.get(bucketIdx);

            if (bucket == null) {
                Validate.validState(child != null); // sanity check
                return child.touch(time, node);
            } else {
                KBucketChangeSet kBucketChangeSet = bucket.touch(time, node);
                BitString kBucketPrefix = bucket.getPrefix();
                Instant kBucketLastUpdateTime = bucket.getLastUpdateTime();
                
                if (kBucketLastUpdateTime.isAfter(lastUpdateTime)) {
                    lastUpdateTime = kBucketLastUpdateTime;
                    bucketUpdateTimes.remove(kBucketPrefix);
                    bucketUpdateTimes.insert(lastUpdateTime, kBucketPrefix);
                }
                
                return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
            }
        }

        public RouteTreeChangeSet stale(Node node) throws LinkConflictException {
            // Other validate checks done by caller, no point in repeating this for an unchanging argument in recursive method
            Id id = node.getId();
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket bucket = kBuckets.get(bucketIdx);

            if (bucket == null) {
                Validate.validState(child != null); // sanity check
                return child.stale(node);
            } else {
                KBucketChangeSet kBucketChangeSet = bucket.stale(node);
                BitString kBucketPrefix = bucket.getPrefix();
                Instant kBucketLastUpdateTime = bucket.getLastUpdateTime();
                
                if (kBucketLastUpdateTime.isAfter(lastUpdateTime)) { // should never change from call to kbucket.stale(), but just incase
                    lastUpdateTime = kBucketLastUpdateTime;
                    bucketUpdateTimes.remove(kBucketPrefix);
                    bucketUpdateTimes.insert(lastUpdateTime, kBucketPrefix);
                }
                
                return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
            }
        }
    }
}
