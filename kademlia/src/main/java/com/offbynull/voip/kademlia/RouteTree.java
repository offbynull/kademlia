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
import java.util.Collection;
import static java.util.Collections.max;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
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

    public List<Activity> find(Id id, int max) {
        Validate.notNull(id);
//        Validate.isTrue(!id.equals(baseNode.getId())); // you can search for yourself, although no point
        Validate.isTrue(id.getBitLength() == baseNode.getId().getBitLength());
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyways

        // Create collection to hold closest nodes -- TreeSet because maintains order and keeps out duplicates
        IdClosenessComparator comparator = new IdClosenessComparator(id);
        TreeSet<Activity> output = new TreeSet<>((x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()));
        
        
        // How does this method work? It grabs nodes from the bucket with the largest possible prefix, and then it begins successfully
        // accessing buckets with smaller and smaller prefixes. For example, assume a 4-bit system where you are 0000...
        //
        //                   0/ \1
        //                   /   \
        //                  /     \
        //                 /        
        //                /          
        //               /             
        //              /              
        //             /                
        //            /                  
        //           /                    
        //         0/ \1                              
        //         /   \                              
        //        /     \                    
        //       /                            
        //      /                              
        //    0/ \1                              
        //    /   \                               
        //  0/\1                                   
        //  *
        //
        // Now lets say you wanted to find the 5 closest nodes (closest in this case means largest possible prefix) to 0010...
        // 
        //
        // 1. Start: The bucket with the largest prefix to node 0010 is the bucket 001x
        //     -- Note that this step is accessing nodes in the routing table with the largest possible prefix (001x)
        //
        // 2. Remove the 3rd bit from the prefix to search all buckets under 00xx (0000/0001):
        //     -- Note that this step is accessing ALL nodes in the routing table with the 2nd largest possible prefix (00xx)
        //
        // 3. Remove the 2nd bit from the prefix to search all buckets under 0xxx (0000/0001/001x/01xx):
        //     -- Note that this step is accessing ALL nodes in the routing table with the 3rd largest possible prefix (0xxx)
        //
        // 4. Remove the 1st bit from the prefix to search all buckets under xxxx (0000/0001/001x/01xx/1xxx):
        //     -- Note that this step is accessing ALL nodes in the routing table with the 4th largest possible prefix (xxxx) AKA no prefix
        // 
        
        // Dump out nodes in bucket with largest matching prefix
        dumpBucketToOutput(id, output, max);
        
        // Dump out nodes in buckets with smaller matching prefixes
        int maxPrefixMatchLen = root.findBucketWithLargestSharedPrefix(id).getPrefix().getBitLength();
        int nextPrefixLen = maxPrefixMatchLen - 1;
        while (nextPrefixLen >= 0 && output.size() < max) {
            dumpAllBucketsWithPrefixToOutput(id, nextPrefixLen, output, max);
        }
        
        
        
        
        
        return new ArrayList<>(output);

        
        // At this point, output will contain the closest nodes that share a prefix. However, if we still have room, re-do the search as if
        // all the buckets accessed were empty -- flipping the bits corresponding to those empty buckets, as per Kademlia's notion of
        // closeness. 
        //
        // IMPORTANT NOTE: If we've more than 2 branches per level, the bit flipping is not bit flipping but changing the bits such that we
        //                 traverse further down the tree. Also, when at the k-bucket with the largest common shared prefix, every k-bucket
        //                 at that level is scanned and sorted by shared common prefix length
        //
        //
        // Why do you want to do this? A few reasons...
        //
        // 1. Highly unbalanced networks
        // 2. Networks in their infancy (very few nodes in the network)
        //
        // Assume a 3-bit system with k = 1. Assume that the network only has nodes in the 11x range present.
        // 
        //                   0/ \1
        //                   /   \
        //                  /     \
        //                 /       \
        //                /         \
        //               /           \ 
        //              /             \
        //             /               \
        //            /                 \
        //           /                   \
        //         0/ \1               0/ \1          
        //         /   \               /   \          
        //        /     \             /     \
        //       /       \           /       \
        //      /         \         /         \
        //    0/ \1     0/ \1     0/ \1     0/ \1
        //    /   \     /   \     /   \     /   \ 
        //  0/\1 0/\1 0/\1 0/\1 0/\1 0/\1 0/\1 0/\1
        //
        // This is a highly unbalanced network
        //
        // Node 111b wants to store a value at 000b, but node 111b has nothing in its 0xxb bucket. The store could be important, so we
        // follow this notion of closeness and flip the top bit, turning 000b to 100b. 100b then becomes the ID to search for. It turns out
        // that 100b isn't present in the network as well, so we  flip the next bit and turn 100b to 110b. 110b exists and is found.
        //
        // How likely is this to happen in a real Kademlia network with a key size of 160-bits? Even if the RNG for node ID generation is
        // biased, it probably won't be biased to the point where we have a massively lop-sided tree. But, networks just starting out with
        // only a few nodes in them may encounter this scenario.
//
//
//
//        while (output.size() < max && lastDumpedLevel.child != null) {
//            int bitOffset = lastDumpedLevel.prefix.getBitLength();
//            BitString lastDumpedLevelBitsForChild = lastDumpedLevel.child.prefix.getBits(bitOffset, lastDumpedLevel.suffixLen);
//            
//            id = id.setBits(bitOffset, lastDumpedLevelBitsForChild);
//            lastDumpedLevel = root.findClosestNodesByLargestSharedPrefix(id, output, max);
//        }
//        
//        return new ArrayList<>(output);
    }

    private void dumpBucketToOutput(Id id, Collection<Activity> output, int max) {
        IdClosenessComparator closenessComparator = new IdClosenessComparator(id);
        if (id.equals(baseNode.getId())) {
            // SPECIAL CASE: If searchId is self, add self to output and continue
            output.add(new Activity(baseNode, Instant.MIN));
        } else {
            // Get bucket associated with search id
            KBucket closestBucket = root.findBucketWithLargestSharedPrefix(id);
            List<Activity> closestBucketNodes = closestBucket.dumpBucket();

            // Sort that bucket based on "closeness"
            closestBucketNodes.sort((x, y) -> closenessComparator.compare(x.getNode().getId(), y.getNode().getId()));

            // Add the appropriate number of elements to fill up output as much as we can (get it as close to max len as possible)
            int remaining = max - output.size();
            int amountToAdd = Math.min(remaining, closestBucketNodes.size());
            output.addAll(closestBucketNodes.subList(0, amountToAdd));
        }
    }

    private void dumpAllBucketsWithPrefixToOutput(Id id, int prefixLen, TreeSet<Activity> output, int max) {
        BitString searchPrefix = id.getBitString().getBits(0, prefixLen);
        if (id.equals(baseNode.getId())) {
            // SPECIAL CASE: If searchId is self, add self to output and continue
            output.add(new Activity(baseNode, Instant.MIN));
        } else {
            // Get all buckets with this prefix
            LinkedList<KBucket> foundKBuckets = new LinkedList<>();
            root.findBucketsWithPrefix(searchPrefix, foundKBuckets);
            
            for (KBucket foundKBucket : foundKBuckets) {
                output.addAll(foundKBucket.dumpBucket());
                // if we've exceeded max, discard thes that are farthest away, keeping only the max closest elements
                while (output.size() > max) {
                    output.pollLast();
                }
            }
        }
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

        public void dumpNodesWithSharedPrefix(Id id, TreeSet<Activity> output, int max) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket targetKBucket = kBuckets.get(bucketIdx);

            if (targetKBucket == null) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we
                // can continue down child to find more buckets with a larger prefix.
                Validate.validState(child != null); // sanity check
                child.dumpNodesWithSharedPrefix(id, output, max);
                
                this.dumpAllNodesWithinBranch(output, max);
            } else {
                // Target kBucket found -- we've reached the bucket with the largest common prefix.
                //
                // How does this work?
                //
                // For example, assume an id space of 6 bits and a routing tree that handles 2 bits at a time (branches 4 times per level).
                // Let's say we're looking for the closest nodes to ID 000110. The largest matching prefix for this is 0001xx, which is
                // found on the second level. Dump the bucket for 0001xx.
                //
                // If we still have room left, then go down branch 0000xx and dump in all the buckets you can find (recursively if the tree)
                // is larger) until you run out of room -- because buckets under 0000xx share the next largest prefix with the ID we're
                // searching for (000110 and 0000xx share the first 3 bits: 000xxx).
                //
                // If we still have even more room after that, we need to come back up the tree and dump remaining branches: 0010xx and
                // 0011xx -- because these two buckets share the next largest prefix prefixes with the ID we're searching for (share the
                // first 2 bits).
                //
                // If we still ahve even more room after that, come up to the first level and target 01xxxx (shares 1 bit), and after that
                // 10xxxx and 11xxxx (shares 0 bits).
                //
                // If you follow this order of doing things, you're ensured that youre list will contain largest matching prefix to least
                // matching prefix.
                //
                //                                   /\
                //                                  / |\
                //                                 /  | \
                //                                /  / \ \
                //                               /   | |  \
                //                              /    | |   \
                //                           00/  01/  \10  \11
                //                            /\
                //                           / |\
                //                          /  | \
                //                         /  / \ \
                //                        /   | |  \
                //                       /    | |   \
                //                    00/  01/  \10  \11
                //                     /\
                //                    / |\
                //                   /  | \
                //                  /  / \ \
                //                 /   | |  \
                //                /    | |   \
                //             00/  01/  \10  \11
                //
                //
                // Can this be generalized to an algorithm?
                //
                // Find the level that has the bucket with the largest matching prefix. Sort the branches at that level by matching prefix.
                // Go through the sorted branches in order...
                //
                //   If it's a bucket: dump it.
                //   If it's a branch: recursively descend down the branch (breadth first) and dump each bucket
                //
                // At this point you're at the same level you started from. Assuming that you still don't have the max elements you want.
                // Go back up parent, sort the branches at that level by matching prefix, and go through the sorted branches in order just
                // like you originally did. BUT don't recurse don't the branch you just returned from!
                //
                //
                kBuckets.stream()
                        .filter(x -> x != null)
                        .sorted((x, y) -> Integer.compare(x.getPrefix().getSharedPrefixLength(id.getBitString()), y.getPrefix().getSharedPrefixLength(id.getBitString())))
//                        .flatMap(x -> x.dumpBucket().stream())
                        .forEachOrdered(output::add);
                
                
                if (child != null) {
                    child.dumpAllNodesWithinBranch(output, max);
                }
            }
        }
        
        public void dumpAllNodesWithinBranch(TreeSet<Activity> output, int max) {
                kBuckets.stream()
                        .filter(x -> x != null)
                        .flatMap(x -> x.dumpBucket().stream())
                        .forEach(output::add);
                
                if (child != null) {
                    child.dumpAllNodesWithinBranch(output, max);
                }
        }

        public KBucket findBucketWithLargestSharedPrefix(Id id) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket targetKBucket = kBuckets.get(bucketIdx);

            if (targetKBucket == null) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we
                // can continue down child to find more buckets with a larger prefix.
                Validate.validState(child != null); // sanity check
                return child.findBucketWithLargestSharedPrefix(id);
            } else {
                // Target kBucket found -- we've reached the end of how far we can go down the routing tree
                return targetKBucket;
            }
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
