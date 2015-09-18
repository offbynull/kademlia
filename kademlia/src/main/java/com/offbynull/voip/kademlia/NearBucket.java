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

import java.util.ArrayList;
import static java.util.Collections.emptyList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class NearBucket {

    private final Id baseId;

    private final NodeNearSet bucket; // nearest nodes to you by id
    private final NodeNearSet peers; // should contain all nodes that aren't stale in the routetree... used to add nodes to bucket if it
                                     // gets less than its max size

    public NearBucket(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0); // what's the point of a 0 size bucket? let it thru anyways

        this.baseId = baseId;
        this.bucket = new NodeNearSet(baseId, maxSize);
        this.peers = new NodeNearSet(baseId, Integer.MAX_VALUE);
    }

    // A peer that has been added to the routing table. okay to call this more than once on same node
    public NearBucketChangeSet touchPeer(Node node) {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        NodeChangeSet networkChangeSet = peers.touch(node);
        NodeChangeSet bucketChangeSet = bucket.touch(node);
        
        return new NearBucketChangeSet(bucketChangeSet, networkChangeSet);
    }
    
    public NearBucketChangeSet touch(Node node) {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());

        
        // Touch the bucket
        NodeChangeSet bucketChangeSet = bucket.touch(node);
        return new NearBucketChangeSet(bucketChangeSet, NodeChangeSet.NO_CHANGE);
    }
    
    // Node has been removed. doesn't matter if its in network or bucket
    public NearBucketChangeSet remove(Node node) {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        
        NodeChangeSet bucketChangeSet = bucket.remove(node);
        NodeChangeSet networkChangeSet = peers.remove(node);
        
        NodeChangeSet applyToBucketRes = applyPeerNodesToBucket();
        Validate.isTrue(applyToBucketRes.viewRemoved().isEmpty()); // sanity check
        
        return new NearBucketChangeSet(
                new NodeChangeSet(applyToBucketRes.viewAdded(), bucketChangeSet.viewRemoved(), emptyList()),
                networkChangeSet);
    }

    public NearBucketChangeSet resize(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        if (maxSize <= bucket.getMaxSize()) {
            // reducing space
            NodeChangeSet res = bucket.resize(maxSize);
            
            // sanity check
            // validate nothing was added or updated -- the only thing that can happen is elements can be removed
            Validate.validState(res.viewAdded().isEmpty());
            Validate.validState(res.viewUpdated().isEmpty());
            
            return new NearBucketChangeSet(res, NodeChangeSet.NO_CHANGE);
        } else {
            // increasing space, so move over stuff from the cache in to new bucket spaces
            NodeChangeSet bucketResizeRes = bucket.resize(maxSize);
            
            // sanity check
            // validate nothing changed with elements in the set -- we're only expanding the size of the bucket
            Validate.validState(bucketResizeRes.viewAdded().isEmpty());
            Validate.validState(bucketResizeRes.viewRemoved().isEmpty());
            Validate.validState(bucketResizeRes.viewUpdated().isEmpty());
            
            
            NodeChangeSet applyToBucketRes = applyPeerNodesToBucket();
            Validate.isTrue(applyToBucketRes.viewRemoved().isEmpty()); // sanity check nothing was removed
            Validate.isTrue(applyToBucketRes.viewUpdated().isEmpty()); // sanity check nothing was removed
            
            return new NearBucketChangeSet(applyToBucketRes, NodeChangeSet.NO_CHANGE);
        }
    }
    
    public List<Node> dumpBucket() {
        return bucket.dump();
    }
    
    private NodeChangeSet applyPeerNodesToBucket() {
        try {
            // If we have no peer nodes, return immediately
            int availablePeers = peers.size();
            if (availablePeers == 0) {
                return NodeChangeSet.NO_CHANGE;
            }

            // If the bucket is full, attempt to apply closest peer node to the bucket
            int unoccupiedBucketSlots = bucket.getMaxSize() - bucket.size();
            if (unoccupiedBucketSlots <= 0) {
                Node closestReplacementNode = peers.dumpNearestAfter(baseId, 1).get(0); // get closest node out of replacements
                try {
                    return bucket.touch(closestReplacementNode);
                } catch (LinkConflictException lce) {
                    // should never happen
                    throw new IllegalStateException(lce);
                }
            }
        
            
            int moveAmount = Math.min(availablePeers, unoccupiedBucketSlots);
            
            // If bucket is empty, copy over as much as we can. Copy over the peer nodes that are closest to your own id.
            if (bucket.size() == 0) {
                List<Node> closestPeers = peers.dumpNearestAfter(baseId, moveAmount);
                for (Node node : closestPeers) {
                    NodeChangeSet ret = bucket.touch(node);
                    Validate.isTrue(ret.viewAdded().size() == 1); // sanity check
                    Validate.isTrue(ret.viewRemoved().isEmpty()); // sanity check
                    Validate.isTrue(ret.viewUpdated().isEmpty()); // sanity check
                }
                
                return NodeChangeSet.added(closestPeers);
            }

            List<Node> bucketNodes = bucket.dump();
            
            List<Node> addedNodes = new ArrayList<>(moveAmount);
            
            // Copy over network nodes that are closer than the closest node to our own id, if we have any.
            Node closestBucketNode = bucketNodes.get(0);
            List<Node> closerPeers = peers.dumpNearestBefore(closestBucketNode.getId(), moveAmount);
            for (Node node : closerPeers) {
                NodeChangeSet ret = bucket.touch(node);
                Validate.isTrue(ret.viewAdded().size() == 1); // sanity check
                Validate.isTrue(ret.viewRemoved().isEmpty()); // sanity check
                Validate.isTrue(ret.viewUpdated().isEmpty()); // sanity check
                addedNodes.add(node);
                moveAmount--;
            }

            // There may be room left, so copy over network nodes that are farther than the farthest node to our own id, if we have any.
            Node farthestBucketNode = bucketNodes.get(bucketNodes.size() - 1);
            List<Node> fartherPeers = peers.dumpNearestAfter(farthestBucketNode.getId(), moveAmount);
            for (Node node : fartherPeers) {
                NodeChangeSet ret = bucket.touch(node);
                Validate.isTrue(ret.viewAdded().size() == 1); // sanity check
                Validate.isTrue(ret.viewRemoved().isEmpty()); // sanity check
                Validate.isTrue(ret.viewUpdated().isEmpty()); // sanity check
                addedNodes.add(node);
            }
            
            return NodeChangeSet.added(addedNodes);
        } catch (LinkConflictException lce) {
            // should never happen
            throw new IllegalStateException(lce);
        }
    }    

    @Override
    public String toString() {
        return "NearBucket{" + "baseId=" + baseId + ", bucket=" + bucket + ", network=" + peers + '}';
    }
}
