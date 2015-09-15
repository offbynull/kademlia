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

    public List<Activity> find(Id id, int max) {
        Validate.notNull(id);
//        Validate.isTrue(!id.equals(baseNode.getId())); // you can search for yourself, although no point
        Validate.isTrue(id.getBitLength() == baseNode.getId().getBitLength());
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyways

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
        // like you originally did. BUT don't recurse down the branch you just returned from!
        //
        //
        
        LinkedList<Activity> output = new LinkedList<>();
        
        
        
        return output;
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
        int parentNumOfBuckets = parent.branches.size();
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
        KBucket splitBucket = ((KBucket) parent.branches.get(splitBucketIdx));
        BitString splitBucketPrefix = splitBucket.getPrefix();
        
        
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
        KBucket[] newBuckets = splitBucket.split(suffixBitCount);
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
        bucketUpdateTimes.remove(splitBucketPrefix);

        // Create new level and set as child
        RouteTreeLevel ret = new RouteTreeLevel(newPrefix, suffixBitCount, new ArrayList<>(Arrays.asList(newBuckets)));
        parent.branches.set(splitBucketIdx, ret);
        
        return ret;
    }
    
    private final class RouteTreeLevel {
        private final BitString prefix;
        private final int suffixLen;

        private final List<Object> branches; // branches can contain KBuckets or RouteTreeLevels that are further down

        private RouteTreeLevel(BitString prefix, int suffixLen, List<Object> branches) {
            this.prefix = prefix;
            this.suffixLen = suffixLen;
            this.branches = branches;
        }

        public void dumpNodesInBucket(BitString searchPrefix, LinkedList<Activity> output) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(searchPrefix.getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix of searchPrefix matches

            int bucketIdx = (int) searchPrefix.getBitsAsLong(prefix.getBitLength(), suffixLen);
            Object branch = branches.get(bucketIdx);

            if (branch instanceof RouteTreeLevel) {
                // Target kBucket is null, which means that nodes with this prefix can be found by continuing down child. In other words, we can
                // continue down child to find more buckets with a larger prefix.
                ((RouteTreeLevel) branch).dumpNodesInBucket(searchPrefix, output);
            } else {
                // Target kBucket found -- we've reached the end of how far we can go down the routing tree
                output.addAll(((KBucket) branch).dumpBucket());
            }
        }
        
        public RouteTreeChangeSet touch(Instant time, Node node) throws LinkConflictException {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Id id = node.getId();
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            Object branch = branches.get(bucketIdx);

            if (branch instanceof RouteTreeLevel) {
                return ((RouteTreeLevel) branch).touch(time, node);
            } else if (branch instanceof KBucket) {
                KBucket bucket = (KBucket) branch;
                KBucketChangeSet kBucketChangeSet = bucket.touch(time, node);
                BitString kBucketPrefix = bucket.getPrefix();
                Instant kBucketLastUpdateTime = bucket.getLastUpdateTime();
                
                if (kBucketLastUpdateTime.isAfter(lastUpdateTime)) {
                    lastUpdateTime = kBucketLastUpdateTime;
                    bucketUpdateTimes.remove(kBucketPrefix);
                    bucketUpdateTimes.insert(lastUpdateTime, kBucketPrefix);
                }
                
                return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
            } else {
                throw new IllegalStateException(); // should never happen
            }
        }

        public RouteTreeChangeSet stale(Node node) throws LinkConflictException {
            // Other validate checks done by caller, no point in repeating this for an unchanging argument in recursive method
            Id id = node.getId();
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            Object branch = branches.get(bucketIdx);

            if (branch instanceof RouteTreeLevel) {
                return ((RouteTreeLevel) branch).stale(node);
            } if (branch instanceof KBucket) {
                KBucket bucket = (KBucket) branch;
                KBucketChangeSet kBucketChangeSet = bucket.stale(node);
                BitString kBucketPrefix = bucket.getPrefix();
                Instant kBucketLastUpdateTime = bucket.getLastUpdateTime();
                
                if (kBucketLastUpdateTime.isAfter(lastUpdateTime)) { // should never change from call to kbucket.stale(), but just incase
                    lastUpdateTime = kBucketLastUpdateTime;
                    bucketUpdateTimes.remove(kBucketPrefix);
                    bucketUpdateTimes.insert(lastUpdateTime, kBucketPrefix);
                }
                
                return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
            } else {
                throw new IllegalStateException(); // should never happen
            }
        }
    }
}
