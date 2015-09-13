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

import com.offbynull.voip.kademlia.RouteTreeSpecificationSupplier.BucketParameters;
import com.offbynull.voip.kademlia.RouteTreeSpecificationSupplier.DepthParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class RouteTree {
    private final Id baseId;
    private final RouteTreeLevel root;
    private final TimeSet<BitString> bucketUpdateTimes;
    
    private Instant lastUpdateTime;
    
    public RouteTree(Id baseId, RouteTreeSpecificationSupplier specSupplier) {
        Validate.notNull(baseId);
        Validate.notNull(specSupplier);

        this.baseId = baseId; // must be set before creating RouteTreeLevels
        this.bucketUpdateTimes = new TimeSet<>();

        root = createRoot(specSupplier);
        RouteTreeLevel child = root;
        while (child != null) {
            child = growParent(child, specSupplier);
        }
        
        this.lastUpdateTime = Instant.MIN;
    }

    public List<Activity> find(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(!id.equals(baseId));
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyways

        ArrayList<Activity> output = new ArrayList<>(max);
        root.findClosestNodes(id, output, max);
        return output;
    }

    public List<Activity> dumpBucket(BitString prefix) {
        Validate.notNull(prefix);
        Validate.isTrue(prefix.getBitLength() < baseId.getBitLength()); // cannot be == or >

        LinkedList<Activity> output = new LinkedList<>();
        root.dumpNodesInBucket(prefix, output);
        return new ArrayList<>(output);
    }

    public RouteTreeChangeSet touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        Validate.isTrue(!id.equals(baseId));
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime

        return root.touch(time, node);
    }

    public RouteTreeChangeSet stale(Node node) throws LinkConflictException {
        Validate.notNull(node);

        Id id = node.getId();
        Validate.isTrue(!id.equals(baseId));
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
            
        return root.stale(node);
    }

    public List<BitString> getBucketsUpdatedBefore(Instant time) {
        return bucketUpdateTimes.getBefore(time, true);
    }
    
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    private static final BitString EMPTY = BitString.createFromString("");

    private RouteTreeLevel createRoot(RouteTreeSpecificationSupplier specSupplier) {
        Validate.notNull(specSupplier);

        
        
        // Get parameters for root
        DepthParameters depthParams = specSupplier.getParameters(EMPTY);
        Validate.isTrue(depthParams != null);

        // Get number of buckets to create for root
        int numOfBuckets = depthParams.getNumberOfBranches();
        Validate.validState(Integer.bitCount(numOfBuckets) == 1); // sanity check numofbuckets is pow of 2
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3

        // Create buckets by creating a 0-sized top bucket and splitting it + resizing each split
        KBucket[] newBuckets = new KBucket(baseId, EMPTY, 0, 0).split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = depthParams.getBucketParametersAtBranch(i);
            int bucketSize = bucketParams.getBucketSize();
            int cacheSize = bucketParams.getCacheSize();
            newBuckets[i].resizeBucket(bucketSize);
            newBuckets[i].resizeCache(cacheSize);
            
            bucketUpdateTimes.insert(newBuckets[i].getLastUpdateTime(), newBuckets[i].getPrefix());
        }

        // Create root
        return new RouteTreeLevel(EMPTY, suffixBitCount, new ArrayList<>(Arrays.asList(newBuckets)));
    }

    private RouteTreeLevel growParent(RouteTreeLevel parent, RouteTreeSpecificationSupplier specSupplier) {
        Validate.notNull(parent);
        Validate.notNull(specSupplier);

        // Calculate which bucket from parent to split
        int parentNumOfBuckets = parent.kBuckets.size();
        Validate.validState(Integer.bitCount(parentNumOfBuckets) == 1); // sanity check numofbuckets is pow of 2
        int parentSuffixBitCount = Integer.bitCount(parentNumOfBuckets - 1); // num of bits e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3
        
        int splitBucketIdx = (int) baseId.getBitString().getBitsAsLong(parent.prefix.getBitLength(), parentSuffixBitCount);
        BitString splitBucketPrefix = parent.kBuckets.get(splitBucketIdx).getPrefix();
        
        

        // Get parameters for new level
        DepthParameters depthParams = specSupplier.getParameters(splitBucketPrefix);
        if (depthParams == null) { // can no longer branch
            return null;
        }

        
        
        // Get number of buckets to create for new level
        int numOfBuckets = depthParams.getNumberOfBranches();
        Validate.validState(Integer.bitCount(numOfBuckets) == 1); // sanity check pow of 2
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 (1000) -- 1000 - 1 = 0111, bitcount(0111) = 3
        
        // Split parent bucket at that branch index
        BitString newPrefix = baseId.getBitString().getBits(0, splitBucketPrefix.getBitLength() + suffixBitCount);
        KBucket[] newBuckets = parent.kBuckets.get(splitBucketIdx).split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = depthParams.getBucketParametersAtBranch(i);
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
                Validate.validState(child == null); // sanity check
                output.addAll(targetKBucket.dumpBucket());
            }
        }

        public void findClosestNodes(Id id, ArrayList<Activity> output, int max) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            KBucket targetKBucket = kBuckets.get(bucketIdx);

            if (targetKBucket == null) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we can
                // continue down child to find more buckets with a larger prefix.
                Validate.validState(child != null); // sanity check
                child.findClosestNodes(id, output, max);

                // If we have more room available, scan buckets at this level and add nodes closes to id in to output
                dumpCloseNodesFromKBucketsOnThisLevel(id, output, max);
            } else {
                // Target kBucket found -- we've reached the end of how far we can go down the routing tree
                Validate.validState(child == null); // sanity check

                // Scan buckets at this level and add nodes closes to id in to output
                dumpCloseNodesFromKBucketsOnThisLevel(id, output, max);
            }
        }

        private void dumpCloseNodesFromKBucketsOnThisLevel(Id id, ArrayList<Activity> output, int max) {
            int remaining = max - output.size();
            if (remaining <= 0) {
                return;
            }

            IdClosenessComparator comparator = new IdClosenessComparator(id);
            kBuckets.stream()
                    .filter(x -> x != null) // skip null buckets -- if a bucket is null, the tree branches down at that prefix
                    .flatMap(x -> x.dumpBucket().stream()) // kbucket to nodes in kbuckets
                    .sorted((x, y) -> -comparator.compare(x.getNode().getId(), y.getNode().getId())) // - to order nodes by biggest prefix first
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
                Validate.validState(child == null); // sanity check

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
                Validate.validState(child == null); // sanity check

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
