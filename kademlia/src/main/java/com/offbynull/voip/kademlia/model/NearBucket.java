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

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

public final class NearBucket {

    private final Id baseId;

    private final NodeBeforeSet beforeBucket; // nearest nodes to you THAT ARE LESSER THAN YOU based on euclidean dist... head = smallest
    private final NodeAfterSet afterBucket; // nearest nodes to you THAT ARE GREATER THAN YOU based on euclidean dist... head = largest
    
    private final TreeMap<Id, Node> cache; // should contain all nodes that aren't stale in the routetree... 0 = smallest

    public NearBucket(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0); // what's the point of a 0 size bucket? let it thru anyways

        this.baseId = baseId;
        
        this.beforeBucket = new NodeBeforeSet(baseId, maxSize);
        this.afterBucket = new NodeAfterSet(baseId, maxSize);
        
        cache = new TreeMap<>(new IdEuclideanMetricComparator(baseId));
    }

    // if cacheable == true, it means that node can be used as a replacement if number of nodes goes under bucket.maxSize
    public NearBucketChangeSet touch(Node node, boolean cacheable) {
        Validate.notNull(node);

        Id nodeId = node.getId();

        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
        InternalValidate.notMatchesBase(baseId, nodeId);

        NodeChangeSet cacheChangeSet = NodeChangeSet.NO_CHANGE;
        if (cacheable) {
            boolean added = cache.putIfAbsent(nodeId, node) == null;
            
            if (added) { // added
                cacheChangeSet = NodeChangeSet.added(node);
            } else { // already exists, so mark it as updated
                cacheChangeSet = NodeChangeSet.updated(node);
            }
        }
        
        
        NodeChangeSet beforeBucketChangeSet = beforeBucket.touch(node);
        NodeChangeSet afterBucketChangeSet = afterBucket.touch(node);
        
        
        return new NearBucketChangeSet(beforeBucketChangeSet, afterBucketChangeSet, cacheChangeSet);
    }
    
    // Node has been removed. doesn't matter if its in network or bucket
    public NearBucketChangeSet remove(Node node) {
        Validate.notNull(node);

        Id nodeId = node.getId();

        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
        InternalValidate.notMatchesBase(baseId, nodeId);
        
        NodeChangeSet cacheChangeSet = NodeChangeSet.NO_CHANGE;
        boolean removedFromCache = cache.remove(nodeId) != null;
        if (removedFromCache) {
            cacheChangeSet = NodeChangeSet.removed(node);
        }
        
        
        // Do for before
        NodeChangeSet beforeBucketChangeSet = beforeBucket.remove(node);
        
        int beforeBucketFreeSpace = beforeBucket.getMaxSize() - beforeBucket.size();
        Iterator<Node> headIt = cache.headMap(baseId, true).descendingMap().values().iterator();
        while (headIt.hasNext() && beforeBucketFreeSpace >= 0) {
            Node replaceNode = headIt.next();
            
            NodeChangeSet tempChangeSet = beforeBucket.touch(replaceNode);
            beforeBucketChangeSet = combineNodeChangeSet(beforeBucketChangeSet, tempChangeSet);
            
            beforeBucketFreeSpace--;
        }
        
        
        // Do for after
        NodeChangeSet afterBucketChangeSet = afterBucket.remove(node);

        int afterBucketFreeSpace = afterBucket.getMaxSize() - afterBucket.size();
        Iterator<Node> tailIt = cache.tailMap(baseId, true).values().iterator();
        while (tailIt.hasNext() && afterBucketFreeSpace >= 0) {
            Node replaceNode = tailIt.next();
            
            NodeChangeSet tempChangeSet = afterBucket.touch(replaceNode);
            afterBucketChangeSet = combineNodeChangeSet(afterBucketChangeSet, tempChangeSet);
            
            afterBucketFreeSpace--;
        }
        
        
        // Return
        return new NearBucketChangeSet(beforeBucketChangeSet, afterBucketChangeSet, cacheChangeSet);
    }

    public List<Node> dumpBeforeBucket() {
        return beforeBucket.dump();
    }

    public List<Node> dumpAfterBucket() {
        return afterBucket.dump();
    }
    
    private NodeChangeSet combineNodeChangeSet(NodeChangeSet one, NodeChangeSet two) {
        return new NodeChangeSet(
                CollectionUtils.union(
                        one.viewAdded(),
                        two.viewAdded()
                ),
                CollectionUtils.union(
                        one.viewRemoved(),
                        two.viewRemoved()
                ),
                CollectionUtils.union(
                        one.viewUpdated(),
                        two.viewUpdated()
                )
        );
    }

    @Override
    public String toString() {
        return "NearBucket{" + "baseId=" + baseId + ", beforeBucket=" + beforeBucket + ", afterBucket=" + afterBucket + ", cache="
                + cache + '}';
    }


}
