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
package com.offbynull.voip.kademlia.model;

import com.offbynull.voip.kademlia.model.RouteTreeBucketStrategy.BucketParameters;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

/**
 * An implementation of Kademlia's routing tree. This is an implementation of a <b>strict</b> routing tree, meaning that it doesn't perform
 * the irregularly bucket splitting mentioned in the original Kademlia paper. The portion of the paper that deals with irregular bucket
 * splitting was never fully understood (discussion on topic can be found here: http://stackoverflow.com/q/32129978/1196226).
 * <p>
 * A strict routing tree is a routing tree that is static (doesn't split k-buckets after creation) and extends all the way done to the ID of
 * the Kademlia node that owns. For example, the routing tree of node 000 would look something like this (assuming that the routing tree
 * branches only 1 bit at a time)...
 * <pre>
 *                0/\1
 *                /  [1xx BUCKET]
 *              0/\1
 *              /  [01x BUCKET]
 *            0/\1
 *       [SELF]  [001 BUCKET]
 * </pre>
 * @author Kasra Faghihi
 */
public final class RouteTree {
    private final Id baseId;
    private final RouteTreeNode root;
    private final TimeSet<BitString> bucketUpdateTimes; // prefix to when the prefix's bucket was last updated (not cache)
    
    private Instant lastTouchTime;
    
    public RouteTree(Id baseId, // because id's are always > 0 in size -- it isn't possible for tree creation to mess up
            RouteTreeBranchStrategy branchSpecStrategy,
            RouteTreeBucketStrategy bucketSpecStrategy) {
        Validate.notNull(baseId);
        Validate.notNull(branchSpecStrategy);
        Validate.notNull(bucketSpecStrategy);
        
        this.baseId = baseId; // must be set before creating RouteTreeLevels
        this.bucketUpdateTimes = new TimeSet<>();

        root = createRoot(branchSpecStrategy, bucketSpecStrategy);
        RouteTreeNode child = root;
        while (child != null) {
            child = growParent(child, branchSpecStrategy, bucketSpecStrategy);
        }
        
        // Special case: the routing tree, if large enough, may have a bucket for baseId. Nothing can ever access that bucket (calls to
        // touch/stale/find with your own ID will result an exception) and it'll always be empty, so remove it from bucketUpdateTimes.
        bucketUpdateTimes.remove(baseId.getBitString());
        
        this.lastTouchTime = Instant.MIN;
    }

    // this will always give you the closest nodes in your routetable, based on the xor metric
    public List<Activity> find(Id id, int max, boolean includeStale) {
        Validate.notNull(id);
        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id); // you should be able to search for closest nodes to yourself
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyways

        IdXorMetricComparator comparator = new IdXorMetricComparator(id);
        TreeSet<Activity> output = new TreeSet<>((x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()));
        
        root.findNodesWithLargestPossiblePrefix(id, output, max, includeStale);
  
        // DONT DO THIS. DO NOT INCLUDE SELF. ADD IT AT LAYERS ABOVE.
//        // We don't keep ourself in the buckets in the routing tree, but we may be one of the closest nodes to id. As such, add ourself to
//        // the output list and evict excess (if any)
//        output.add(new Activity(baseNode, Instant.MIN));
//        while (output.size() > max) {
//            output.pollLast();
//        }
        
        return new ArrayList<>(output);
    }
    
    // used for testing
    List<Activity> dumpBucket(BitString prefix) {
        Validate.notNull(prefix);
        Validate.isTrue(prefix.getBitLength() < baseId.getBitLength()); // cannot be == or >

        KBucket bucket = root.getBucketForPrefix(prefix);        
        return bucket.dumpBucket(true, true, false);
    }
    
    public List<BitString> dumpBucketPrefixes() {
        List<BitString> output = new LinkedList<>();
        root.dumpAllBucketPrefixes(output);
        return new ArrayList<>(output);
    }

    public RouteTreeChangeSet touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        InternalValidate.notMatchesBase(baseId, id);
        
        InternalValidate.forwardTime(lastTouchTime, time); // time must be >= lastUpdatedTime
        lastTouchTime = time;

        KBucket bucket = root.getBucketFor(node.getId());
        KBucketChangeSet kBucketChangeSet = bucket.touch(time, node);
        BitString kBucketPrefix = bucket.getPrefix();

        // insert last bucket activity time in to bucket update times... it may be null if bucket has never been accessed, in which case
        // we insert MIN instead
        Instant lastBucketActivityTime = bucket.getLatestBucketActivityTime();
        if (lastBucketActivityTime == null) {
            lastBucketActivityTime = Instant.MIN;
        }
        bucketUpdateTimes.remove(kBucketPrefix);
        bucketUpdateTimes.insert(lastBucketActivityTime, kBucketPrefix);

        return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
    }

    public RouteTreeChangeSet stale(Node node) {
        Validate.notNull(node);

        Id id = node.getId();
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        InternalValidate.notMatchesBase(baseId, id);
            
        KBucket bucket = root.getBucketFor(node.getId());
        KBucketChangeSet kBucketChangeSet = bucket.stale(node);
        BitString kBucketPrefix = bucket.getPrefix();

                // insert last bucket activity time in to bucket update times... it may be null if bucket has never been accessed, in which
        // case we insert MIN instead
        //
        // note that marking a node as stale may have replaced it in the bucket with another node in the cache. That cache node
        // could have an older time than the stale node, meaning that bucketUpdateTimes may actually be older after the replacement!
        Instant lastBucketActivityTime = bucket.getLatestBucketActivityTime();
        if (lastBucketActivityTime == null) {
            lastBucketActivityTime = Instant.MIN;
        }
        bucketUpdateTimes.remove(kBucketPrefix);
        bucketUpdateTimes.insert(lastBucketActivityTime, kBucketPrefix);

        return new RouteTreeChangeSet(kBucketPrefix, kBucketChangeSet);
    }

    // Disable for now -- not being used
//    public void lock(Node node) {
//        Validate.notNull(node);
//
//        Id id = node.getId();
//        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id);
//            
//        root.getBucketFor(node.getId()).lock(node);
//    }
//
//    public void unlock(Node node) {
//        Validate.notNull(node);
//
//        Id id = node.getId();
//        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id);
//            
//        root.getBucketFor(node.getId()).unlock(node);
//    }

    public List<BitString> getStagnantBuckets(Instant time) { // is inclusive
        Validate.notNull(time);
        
        List<BitString> prefixes = bucketUpdateTimes.getBefore(time, true);
        return prefixes;
    }
    
    private static final BitString EMPTY = BitString.createFromString("");

    private RouteTreeNode createRoot(
            RouteTreeBranchStrategy branchSpecStrategy,
            RouteTreeBucketStrategy bucketSpecStrategy) {
        Validate.notNull(branchSpecStrategy);
        Validate.notNull(bucketSpecStrategy);


        // Get number of branches/buckets to create for root
        int numOfBuckets = branchSpecStrategy.getBranchCount(EMPTY);
        Validate.isTrue(numOfBuckets >= 0, "Branch cannot be negative, was %d", numOfBuckets);
        Validate.isTrue(numOfBuckets != 0, "Root of tree must contain at least 1 branch, was %d", numOfBuckets);
        Validate.isTrue(Integer.bitCount(numOfBuckets) == 1, "Branch count must be power of 2");
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3
        Validate.isTrue(suffixBitCount <= baseId.getBitLength(),
                "Attempting to branch too far (in root) %d bits extends past %d bits", suffixBitCount, baseId.getBitLength());

        
        // Create buckets by creating a 0-sized top bucket and splitting it + resizing each split
        KBucket[] newBuckets = new KBucket(baseId, EMPTY, 0, 0).split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = bucketSpecStrategy.getBucketParameters(newBuckets[i].getPrefix());
            int bucketSize = bucketParams.getBucketSize();
            int cacheSize = bucketParams.getCacheSize();
            newBuckets[i].resizeBucket(bucketSize);
            newBuckets[i].resizeCache(cacheSize);
            
            // insert last bucket activity time in to bucket update times... it may be null if bucket has never been accessed, in which case
            // we insert MIN instead
            Instant lastBucketActivityTime = newBuckets[i].getLatestBucketActivityTime();
            if (lastBucketActivityTime == null) {
                lastBucketActivityTime = Instant.MIN;
            }
            bucketUpdateTimes.insert(lastBucketActivityTime, newBuckets[i].getPrefix());
        }

        // Create root
        return new RouteTreeNode(EMPTY, suffixBitCount, newBuckets);
    }

    private RouteTreeNode growParent(RouteTreeNode parent,
            RouteTreeBranchStrategy branchSpecStrategy,
            RouteTreeBucketStrategy bucketSpecStrategy) {
        Validate.notNull(parent);
        Validate.notNull(branchSpecStrategy);
        Validate.notNull(bucketSpecStrategy);

        
        // Calculate which bucket from parent to split
        int parentNumOfBuckets = parent.getBranchCount();
        Validate.validState(Integer.bitCount(parentNumOfBuckets) == 1); // sanity check numofbuckets is pow of 2
        int parentPrefixBitLen = parent.getPrefix().getBitLength(); // num of bits in parent's prefix
        int parentSuffixBitCount = Integer.bitCount(parentNumOfBuckets - 1); // num of bits in parent's suffix
                                                                             // e.g. 8 --> 1000 - 1 = 0111, bitcount(0111) = 3
        
        if (parentPrefixBitLen + parentSuffixBitCount >= baseId.getBitLength()) { // should never be >, only ==, but just in case
            // The parents prefix length + the number of bits the parent used for buckets > baseId's length. As such, it isn't possible to
            // grow any further, so don't even try.
            return null;
        }
        
        int splitBucketIdx = (int) baseId.getBitString().getBitsAsLong(parentPrefixBitLen, parentSuffixBitCount);
        KBucket splitBucket = parent.getBranch(splitBucketIdx).getItem();
        BitString splitBucketPrefix = splitBucket.getPrefix();
        
        
        // Get number of buckets to create for new level
        int numOfBuckets = branchSpecStrategy.getBranchCount(splitBucketPrefix);
        Validate.isTrue(numOfBuckets >= 2, "Branch count must be atleast 2, was %d", numOfBuckets);
        Validate.isTrue(Integer.bitCount(numOfBuckets) == 1, "Branch count must be power of 2");
        int suffixBitCount = Integer.bitCount(numOfBuckets - 1); // num of bits   e.g. 8 (1000) -- 1000 - 1 = 0111, bitcount(0111) = 3
        Validate.isTrue(splitBucketPrefix.getBitLength() + suffixBitCount <= baseId.getBitLength(),
                "Attempting to branch too far %s with %d bits extends past %d bits", splitBucketPrefix, suffixBitCount,
                baseId.getBitLength());
        
        
        // Split parent bucket at that branch index
        BitString newPrefix = baseId.getBitString().getBits(0, parentPrefixBitLen + suffixBitCount);
        KBucket[] newBuckets = splitBucket.split(suffixBitCount);
        for (int i = 0; i < newBuckets.length; i++) {
            BucketParameters bucketParams = bucketSpecStrategy.getBucketParameters(newBuckets[i].getPrefix());
            int bucketSize = bucketParams.getBucketSize();
            int cacheSize = bucketParams.getCacheSize();
            newBuckets[i].resizeBucket(bucketSize);
            newBuckets[i].resizeCache(cacheSize);

            Instant lastBucketActivityTime = newBuckets[i].getLatestBucketActivityTime();
            if (lastBucketActivityTime == null) {
                lastBucketActivityTime = Instant.MIN;
            }
            bucketUpdateTimes.insert(lastBucketActivityTime, newBuckets[i].getPrefix());
        }

        // Get rid of parent bucket we just split. It branches down at that point, and any nodes that were contained within will be in the
        // newly created buckets
        bucketUpdateTimes.remove(splitBucketPrefix);

        // Create new level and set as child
        RouteTreeNode newNode = new RouteTreeNode(newPrefix, suffixBitCount, newBuckets);
        
        parent.setBranch(splitBucketIdx, new RouteTreeNodeBranch(newNode));
        
        return newNode;
    }
}
