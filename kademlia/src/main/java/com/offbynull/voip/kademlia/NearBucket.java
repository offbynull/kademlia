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
    private final NodeNearSet replacements; // should contain all nodes that aren't stale in the routetree... used to add nodes to bucket if it
                                     // gets less than its max size

    public NearBucket(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0); // what's the point of a 0 size bucket? let it thru anyways

        this.baseId = baseId;
        this.bucket = new NodeNearSet(baseId, maxSize);
        this.replacements = new NodeNearSet(baseId, Integer.MAX_VALUE);
    }

    // A peer that has been added to the routing table. okay to call this more than once on same node
    public NearBucketChangeSet replacementNode(Node node) throws LinkConflictException {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        // Touch peer nodes
        NodeChangeSet networkChangeSet = replacements.touch(node);
        List<Node> movedInToBucket = fillMissingBucketSlotsWithPeers();
        
        return new NearBucketChangeSet(NodeChangeSet.added(movedInToBucket), networkChangeSet);
    }
    
    public NearBucketChangeSet touch(Node node) throws LinkConflictException {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());

        
        // Touch the bucket
        NodeChangeSet bucketChangeSet = bucket.touch(node);
        return new NearBucketChangeSet(bucketChangeSet, NodeChangeSet.NO_CHANGE);
    }
    
    // Node has been removed. doesn't matter if its in network or bucket
    public NearBucketChangeSet remove(Node node) throws LinkConflictException {
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        
        NodeChangeSet bucketChangeSet = bucket.remove(node);
        NodeChangeSet networkChangeSet = replacements.remove(node);
        
        List<Node> movedInToBucket = fillMissingBucketSlotsWithPeers();
        
        return new NearBucketChangeSet(
                new NodeChangeSet(movedInToBucket, bucketChangeSet.viewRemoved(), emptyList()),
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
            NodeChangeSet res = bucket.resize(maxSize);
            
            // sanity check
            // validate nothing changed with elements in the set -- we're only expanding the size of the bucket
            Validate.validState(res.viewAdded().isEmpty());
            Validate.validState(res.viewRemoved().isEmpty());
            Validate.validState(res.viewUpdated().isEmpty());
            
            
            List<Node> movedInToBucket = fillMissingBucketSlotsWithPeers();
            
            return new NearBucketChangeSet(NodeChangeSet.added(movedInToBucket), NodeChangeSet.NO_CHANGE);
        }
    }
    
    public List<Node> dumpBucket() {
        return bucket.dump();
    }
    
    private List<Node> fillMissingBucketSlotsWithPeers() {
        int unoccupiedBucketSlots = bucket.getMaxSize() - bucket.size();
        int availablePeers = replacements.size();
        if (unoccupiedBucketSlots <= 0 || availablePeers == 0) {
            return emptyList();
        }
        
        int moveAmount = Math.min(availablePeers, unoccupiedBucketSlots);
        
        try {
            // If we have nothing in our bucket, copy over as much as we can. Copy over the network nodes that are closest to your own id.
            if (bucket.size() == 0) {
                List<Node> closestPeers = replacements.dumpNearestAfter(baseId, moveAmount);
                for (Node node : closestPeers) {
                    bucket.touch(node);
                }
                
                return emptyList();
            }

            List<Node> bucketNodes = bucket.dump();
            
            List<Node> addedNodes = new ArrayList<>(moveAmount);
            
            // Copy over network nodes that are closer than the closest node to our own id, if we have any.
            Node closestBucketNode = bucketNodes.get(0);
            List<Node> closerPeers = replacements.dumpNearestBefore(closestBucketNode.getId(), moveAmount);
            for (Node node : closerPeers) {
                bucket.touch(node);
                addedNodes.add(node);
                moveAmount--;
            }

            // There may be room left, so copy over network nodes that are farther than the farthest node to our own id, if we have any.
            Node farthestBucketNode = bucketNodes.get(bucketNodes.size() - 1);
            List<Node> fartherPeers = replacements.dumpNearestAfter(farthestBucketNode.getId(), moveAmount);
            for (Node node : fartherPeers) {
                bucket.touch(node);
                addedNodes.add(node);
            }
            
            return addedNodes;
        } catch (LinkConflictException lce) {
            // should never happen
            throw new IllegalStateException(lce);
        }
    }    

    @Override
    public String toString() {
        return "NearBucket{" + "baseId=" + baseId + ", bucket=" + bucket + ", network=" + replacements + '}';
    }
}
