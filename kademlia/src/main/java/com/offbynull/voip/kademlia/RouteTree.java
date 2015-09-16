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
import java.util.Collections;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

public final class RouteTree {
    private final Node baseNode;
    private final TreeNode root;
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
        TreeNode child = root;
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

        IdClosenessComparator comparator = new IdClosenessComparator(id);
        TreeSet<Activity> output = new TreeSet<>((x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()));
        
        root.findNodesWithLargestPossiblePrefix(id, output, max);
  
        // DONT DO THIS. DO NOT INCLUDE SELF. ADD IT AT LAYERS ABOVE.
//        // We don't keep ourself in the buckets in the routing tree, but we may be one of the closest nodes to id. As such, add ourself to
//        // the output list and evict excess (if any)
//        output.add(new Activity(baseNode, Instant.MIN));
//        while (output.size() > max) {
//            output.pollLast();
//        }
        
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

    private TreeNode createRoot(
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
        List<TreeBranch> newBranches = new ArrayList<>(newBuckets.length);
        Arrays.stream(newBuckets)
                .map(x -> new BucketTreeBranch(x))
                .forEachOrdered(newBranches::add);
        return new TreeNode(EMPTY, suffixBitCount, newBranches);
    }

    private TreeNode growParent(TreeNode parent,
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
        KBucket splitBucket = parent.branches.get(splitBucketIdx).getItem();
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
        List<TreeBranch> newBranches = new ArrayList<>(newBuckets.length);
        Arrays.stream(newBuckets)
                .map(x -> new BucketTreeBranch(x))
                .forEachOrdered(newBranches::add);
        TreeNode newNode = new TreeNode(newPrefix, suffixBitCount, newBranches);
        
        parent.branches.set(splitBucketIdx, new NodeTreeBranch(newNode));
        
        return newNode;
    }
    
    private final class TreeNode {
        private final BitString prefix;
        private final int suffixLen;

        private final List<TreeBranch> branches; // branches can contain KBuckets or RouteTreeLevels that are further down

        private TreeNode(BitString prefix, int suffixLen, List<TreeBranch> branches) {
            this.prefix = prefix;
            this.suffixLen = suffixLen;
            this.branches = branches;
        }

        // id is the id we're trying to find
        // treeset compares against id
        public void findNodesWithLargestPossiblePrefix(Id id, TreeSet<Activity> output, int max) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            // Recursively go down the until you find the branch with the largest matching prefix to ID. Once you find it, call
            // dumpAllNodesUnderTreeNode, and as you pop back up call dumpAllNodesUnderTreeNode again (making sure to not recurse back in to
            // the branch you're coming out of).
            
            int traverseIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            TreeBranch traverseBranch = branches.get(traverseIdx);
            BitString traversePrefix = traverseBranch.getPrefix(); //id.getBitString().getBits(0, prefix.getBitLength() + suffixLen);

            if (traverseBranch instanceof NodeTreeBranch) {
                TreeNode treeNode = traverseBranch.getItem();
                treeNode.findNodesWithLargestPossiblePrefix(id, output, max);
                
                dumpAllNodesUnderTreeNode(id, output, max, singleton(traversePrefix));
            } else if (traverseBranch instanceof BucketTreeBranch) {
                dumpAllNodesUnderTreeNode(id, output, max, emptySet());
            } else {
                throw new IllegalStateException(); // should never happen
            }
        }

        // id is the id we're trying to find
        // treeset compares against id
        public void dumpAllNodesUnderTreeNode(Id id, TreeSet<Activity> output, int max, Set<BitString> skipPrefixes) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            
            // No more room in bucket? just leave right away.
            if (output.size() >= max) {
                return;
            }

            // Sort branches at this treenode by how close the are to the ID we're searching for... Go through the sorted branches in
            // order...
            //
            //   If it's a bucket: dump it.
            //   If it's a branch: recurse in to the branch and repeat
            //
            ArrayList<TreeBranch> sortedBranches = new ArrayList<>(branches);
            Collections.sort(sortedBranches, new PrefixClosenessComparator(id, prefix.getBitLength(), suffixLen));
            
            // What is the point of taking in an ID and sorting the branches in this tree node such that the we access the "closer" prefixes
            // first? We want to access the branches that are closer to the suffix of the ID first because ...
            //
            //
            // 1. Given the same prefix, we don't end up accessing the exact same set of nodes given. For example...
            //
            //      0/\1
            //      /  EMPTY
            //    0/\1
            //    /  FULL
            //  0/\1
            // ME  FULL
            //
            // Assume the routing tree above. We want to route to node 111, but bucket 1xx is empty. We then go down the other branch and
            // start grabbing nodes starting with prefix 0xx. We then use the suffix of 111 (x11) to determine which branches to traverse
            // down first for our 0xx nodes to return. We do this because we don't want to return the same set of nodes everytime someone
            // tries to access a 1xx node and we have an empty branch.
            //
            // For example...
            // if someone wanted 111 and 1xx was empty, path to search under 0xx would be 011, then 001, then 000.
            // if someone wanted 101 and 1xx was empty, path to search under 0xx would be 001, then 000, then 011.
            //
            // If we did something like a depth-first search, we'd always target 000 first, then 001, then 011. We don't want to do that
            // because we don't want to return the same set of nodes everytime. It would end up being an undue burden on those nodes.
            //
            //
            //
            // 2. Remember our notion of closeness: XOR and normal integer less-than to see which is closer. So for example, lets say we're
            // looking for ID 111110 and the prefix at this point in the tree is is 110xxx. Even though the prefix 110 doesn't match, we
            // still want to match as closely to the remaining suffix as possible, because when we XOR those extra 0's at the beginning of 
            // the suffix mean that we're closer.
            //
            // For example...
            //
            // This tree node has the prefix 110xxx and the ID we're searching for is 111110. There are 2 branches at this tree node:
            // 1100xx and 1101xx
            //
            //      110xxx
            //        /\
            //       /  \
            //      /    \
            //   0 /      \ 1
            //    /        \
            // 1100xx    1101xx
            //
            // We know that for ID 111110, the IDs under 1101xx WILL ALWAYS BE CLOSER than the IDs at 1100xx.
            //
            // XORing with the 1100xx bucket ... XOR(111110, 1100xx) = 0011xx
            // 
            // XORing with the 1101xx bucket ... XOR(111110, 1101xx) = 0010xx
            //
            //
            //     Remember how < works... go compare each single bit from the beginning until you come across a pair of bits that aren't
            //     equal (one is 0 and the other is 1). The ID with 0 at that position is less-than the other one.
            //
            //
            // The one on the bottom (1101xx) will ALWAYS CONTAIN CLOSER IDs...
            //
            // An example ID in top:    110011 ... XOR(111110, 110011) = 001101 = 13
            // An exmaple ID in bottom: 110100 ... XOR(111110, 110100) = 001010 = 9
            // 
                
            for (TreeBranch sortedBranch : sortedBranches) {
                if (skipPrefixes.contains(sortedBranch.getPrefix())) {
                    continue;
                }
                
                if (sortedBranch instanceof NodeTreeBranch) {
                    TreeNode node = sortedBranch.getItem();
                    node.dumpAllNodesUnderTreeNode(id, output, max, emptySet()); // dont propogate skipPrefixes -- not relevent deeper
                    
                    // Bucket's full after dumping nodes in that branch. No point in continued processing.
                    if (output.size() >= max) {
                        return;
                    }
                } else if (sortedBranch instanceof BucketTreeBranch) {
                    KBucket bucket = sortedBranch.getItem();
                    output.addAll(bucket.dumpBucket());
                    
                    // Bucket's full after that add. No point in continued processing.
                    if (output.size() >= max) {
                        // If we have more than max elements from that last add, start evicting farthest away nodes
                        while (output.size() > max) {
                            output.pollLast();
                        }
                        return;
                    }
                } else {
                    throw new IllegalStateException(); // should never happen
                }
            }
        }

        public void dumpNodesInBucket(BitString searchPrefix, LinkedList<Activity> output) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(searchPrefix.getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix of searchPrefix matches

            int bucketIdx = (int) searchPrefix.getBitsAsLong(prefix.getBitLength(), suffixLen);
            TreeBranch branch = branches.get(bucketIdx);

            if (branch instanceof NodeTreeBranch) {
                TreeNode node = branch.getItem();
                node.dumpNodesInBucket(searchPrefix, output);
            } else if (branch instanceof BucketTreeBranch) {
                KBucket bucket = branch.getItem();
                output.addAll(bucket.dumpBucket());
            } else {
                throw new IllegalStateException(); // should never happen
            }
        }
        
        public RouteTreeChangeSet touch(Instant time, Node node) throws LinkConflictException {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Id id = node.getId();
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

            int bucketIdx = (int) id.getBitsAsLong(prefix.getBitLength(), suffixLen);
            TreeBranch branch = branches.get(bucketIdx);

            if (branch instanceof NodeTreeBranch) {
                TreeNode treeNode = branch.getItem();
                return treeNode.touch(time, node);
            } else if (branch instanceof BucketTreeBranch) {
                KBucket bucket = branch.getItem();
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
            TreeBranch branch = branches.get(bucketIdx);

            if (branch instanceof NodeTreeBranch) {
                TreeNode treeNode = branch.getItem();
                return treeNode.stale(node);
            } if (branch instanceof BucketTreeBranch) {
                KBucket bucket = branch.getItem();
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
    
    
    private static interface TreeBranch {
        BitString getPrefix();
        <T> T getItem(); // why type parameter? hack to prevent explicit casting
    }
    
    private static final class BucketTreeBranch implements TreeBranch {

        private final KBucket kBucket;

        public BucketTreeBranch(KBucket kBucket) {
            Validate.notNull(kBucket);
            this.kBucket = kBucket;
        }
        
        @Override
        public BitString getPrefix() {
            return kBucket.getPrefix();
        }

        @Override
        @SuppressWarnings("unchecked")
        public KBucket getItem() {
            return kBucket;
        }
    }

    private static final class NodeTreeBranch implements TreeBranch {

        private final TreeNode node;

        public NodeTreeBranch(TreeNode node) {
            Validate.notNull(node);
            this.node = node;
        }
        
        @Override
        public BitString getPrefix() {
            return node.prefix;
        }

        @Override
        @SuppressWarnings("unchecked")
        public TreeNode getItem() {
            return node;
        }
    }
    
    private static final class PrefixClosenessComparator implements Comparator<TreeBranch> {
        // This is a hacky way to compare bitstrings using the XOR metric intended for IDs
        private final int prefixLen;
        private final int suffixLen;
        private final IdClosenessComparator partialIdClosenessComparator;

        public PrefixClosenessComparator(Id id, int prefixLen, int suffixLen) {
            Validate.notNull(id);
            Validate.isTrue(prefixLen >= 0);
            Validate.isTrue(suffixLen > 0);
            Validate.isTrue(id.getBitLength() >= prefixLen + suffixLen);
            
            this.prefixLen = prefixLen;
            this.suffixLen = suffixLen;
            
            Id partialId = Id.create(id.getBitString().getBits(prefixLen, suffixLen));
            this.partialIdClosenessComparator = new IdClosenessComparator(partialId);
        }

        @Override
        public int compare(TreeBranch o1, TreeBranch o2) {
            Validate.isTrue(o1.getPrefix().getBitLength() == prefixLen + suffixLen);
            Validate.isTrue(o2.getPrefix().getBitLength() == prefixLen + suffixLen);
            
            return partialIdClosenessComparator.compare(
                    Id.create(o1.getPrefix().getBits(prefixLen, suffixLen)),
                    Id.create(o2.getPrefix().getBits(prefixLen, suffixLen)));
        }
        
    }
}
