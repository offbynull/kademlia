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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
        // Find the treenode that has the bucket with the largest matching prefix. Sort the branches at that treenode by matching prefix.
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
        
        root.findNodesWithLargestPossiblePrefix(id, output, max);
        
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

        public void findNodesWithLargestPossiblePrefix(Id id, LinkedList<Activity> output, int max) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

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

        public void dumpAllNodesUnderTreeNode(Id id, LinkedList<Activity> output, int max, Set<BitString> skipPrefixes) {
            // Other validation checks on args done by caller, no point in repeating this for an unchanging argument in recursive method
            Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches
            
            // No more room in bucket? just leave right away.
            if (output.size() >= max) {
                return;
            }

            // What is the point of taking in an ID and doing this preferedPrefix calculation?? When we dump all the nodes in a branch, we
            // want to access the branches that are closer to the suffix of the ID first. We do this for multiple reasons:
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
            // 2. It meshes well with the "notion of closeness" we derived in IdClosenessComparator, which states that...
            // 
            // Assume nodes A, B, and C. According to Kademlia's notion of distance, the larger the prefix shared between two nodes, the
            // closer those nodes are to eachother. So if distance(A,B) == distance(A,C), it means that A, B, and C all share the same
            // prefix and as such are equal distance from each other. 
            //
            // IdClosenessComparator extends this such that, given that B and C share the same common prefix with A, we start successively
            // flipping bits in the suffix until one of them ends up having a larger prefix. The one with the larger prefix after the
            // bit flip(s) will be the closer one.
            //
            // Now assume the same scenario shown in the example above...
            //
            //      0/\1
            //      /  EMPTY
            //    0/\1
            //    /  FULL
            //  0/\1
            // ME  FULL
            //
            // We want to route to 111 but 111 is empty, so we flip to the other branch and take the suffix from 11, meaning that we're now
            // attempting to go to 011. This is exactly what our "notion of closeness" describes above... we have no matching prefix (there
            // are no nodes in 1xx) bucket, so we've "flipped" our first bit (by going to the adjacent branch: 0xx) and now we're searching
            // for 011 instead. Now, if bucket 01x were empty as well, we'd "flip" the second bit (by going to the adjacent branch: 00x) and
            // searching for 001... and so on and so forth.
            //
            // This notion of closeness becomes useful when we're dealing with highly unbalanced trees or networks in their infancy.
            //
            // For example, imagine the following tree. It's representative of a network that's either highly unbalanced on in its infancy
            // (not many nodes available).
            //
            //         0/ \1
            //         /   \
            //        /     \
            //       /       \
            //      /         \
            //    0/ \1     0/ \1
            //    /   \     /   \ 
            //  0/\1 0/\1 0/\1 0/\1
            //  *          * *  * *
            //
            // All the 1xx nodes are in the network, and node 000 is also in the network. Since k=1, each bucket can only hold 1 node. Node
            // 000 can only maintain 1 contact out of all nodes that have prefix 1xx, and the 1xx nodes can only maintain 1 contact in their
            // 0xx bucket.
            //
            // Lets say that thus far only 111 know about 000 each other. 000 has 111 in its 1xx bucket, and 111 has 000 in its 0xx bucket.
            // NO OTHER 1xx nodes know about 000, and as such their 0xx buckets are empty.
            // 
            // Now, if node 110 wanted to route to 000, it will never get that opportunity...
            // 
            // - 110 will look in to the bucket that 000b should be in and will see that it's empty. It will return the node "closest" to
            //   000b: 100b (see section above on "Notion of Closeness" to see how this is calculated).
            // - 100b will also look in to the bucket that 000 should be in and will see that it's empty. It has no where closer to route
            //   to, so it sticks with itself.
            //
            // We can solve this problem by having a special bucket that maintains the closest nodes (as defined by our notion of
            // closeness). So for example, node 000 would maintain the 1 closest node it knows of to it. Let's say node 000 has node 111 in
            // its node bucket, but other 1xx nodes start touching it as well. Node 000's 1xx bucket will stay the same, but its special
            // bucket will get updated such that it holds on to the closest nodes...
            //
            // So, assume that 111 is in 000's 1xx bucket and it's in the special bucket as well. Assume the special bucket only has a size
            // of 1.
            //
            // 1. 101 touches node 000 -- We compare the node that just touched us (101) against the node in our special bucket that
            //                            maintains the closest nodes... using our "notion of closeness" metric, it turns out that 101 is
            //                            closer to us (000) then 101, so we put 101 and evict 111.
            //
            // 2. 100 touches node 000 -- We compare the node that just touched us (100) against the node in our special bucket that
            //                            maintains the closest nodes... using our "notion of closeness" metric, it turns out that 100 is
            //                            closer to us (000) then 101, so we put 100 and evict 101.
            //
            // We send bucket refreshed periodically to this special bucket, so node 100 will always point to us in its 0xx bucket. Now, if
            // any other 1xx node wanted to get to 000 but its 0xx bucket was empty, it would route to 100 instead (remember notion of
            // closeness, bits after shared common prefix, 0 in this case, start getting flipped until a match can be found), and 100 would
            // route to 000 (we're in its 0xx bucket because of our special bucket of closest nodes).
            //
            BitString preferedPrefix = id.getBitString().getBits(0, prefix.getBitLength() + suffixLen);
            ArrayList<TreeBranch> sortedBranches = new ArrayList<>(branches);
            Collections.sort(sortedBranches,
                    (x, y) -> {
                        return -Integer.compare(
                                preferedPrefix.getSharedPrefixLength(x.getPrefix()),
                                preferedPrefix.getSharedPrefixLength(y.getPrefix()));
                    }
            );
                
            for (TreeBranch sortedBranch : sortedBranches) {
                if (skipPrefixes.contains(sortedBranch.getPrefix())) {
                    continue;
                }
                
                if (sortedBranch instanceof NodeTreeBranch) {
                    TreeNode node = sortedBranch.getItem();
                    node.dumpAllNodesUnderTreeNode(id, output, max, emptySet()); // dont propogate skipPrefixes -- not relevent deeper
                } else if (sortedBranch instanceof BucketTreeBranch) {
                    KBucket bucket = sortedBranch.getItem();
                    output.addAll(bucket.dumpBucket());
                    
                    // Bucket's full after that add. No point in continued processing.
                    if (output.size() >= max) {
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
}
