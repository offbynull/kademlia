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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

public final class Router {
    private final Id baseId;
    private final RouteTree routeTree;
    private final NearBucket nearBucket;
    
    private Instant lastTouchTime;
    
    public Router(Id baseId,
            RouteTreeBranchSpecificationSupplier branchSpecSupplier,
            RouteTreeBucketSpecificationSupplier bucketSpecSupplier,
            int maxNearNodes) {
        Validate.notNull(baseId);
        Validate.notNull(branchSpecSupplier);
        Validate.notNull(bucketSpecSupplier);
        Validate.isTrue(maxNearNodes >= 0);
        
        this.baseId = baseId;
        this.routeTree = new RouteTree(baseId, branchSpecSupplier, bucketSpecSupplier);
        this.nearBucket = new NearBucket(baseId, maxNearNodes);
        this.lastTouchTime = Instant.MIN;
    }

    public Router(Id baseId, int branchesPerLevel, int maxNodesPerBucket, int maxCacheNodesPerBucket, int maxNearNodes) {
        this(baseId,
                new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                maxNearNodes);
    }
    
    public RouterChangeSet touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        InternalValidate.forwardTime(lastTouchTime, time); // time must be >= lastUpdatedTime
        this.lastTouchTime = time;
        
        Id nodeId = node.getId();
        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
        InternalValidate.notMatchesBase(baseId, nodeId);

        
        
        // Touch routing tree + apply changes to nearbucket
        RouteTreeChangeSet routeTreeChangeSet = routeTree.touch(time, node);
        NearBucketChangeSet nearBucketChangeSet = synchronizeChangesFromRouteTreeToNearBucket(routeTreeChangeSet);

        // In case it wasn't added from the sync (sync will try to put it in near bucket's cache if it was added/update in the router, which
        // may also put it in to near bucket's bucket if it's part of the closest nodes), then explictly try to add it here
        if (routeTreeChangeSet.getKBucketChangeSet().getBucketChangeSet().viewAdded().isEmpty()
                && routeTreeChangeSet.getKBucketChangeSet().getBucketChangeSet().viewUpdated().isEmpty()) {
            NearBucketChangeSet nearBucketChangeSetFromTouch = nearBucket.touch(node, false);
            nearBucketChangeSet = combineNearBucketChangeSets(nearBucketChangeSet, nearBucketChangeSetFromTouch);
        }
        
        return new RouterChangeSet(routeTreeChangeSet, nearBucketChangeSet);
    }
    
    
    public List<BitString> dumpBucketPrefixes() {
        return routeTree.dumpBucketPrefixes();
    }
    
    // DOES NOT RETURN SELF
    public List<Node> find(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(max >= 0); // why would anyone want 0 items returned? let thru anyways
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        // do not stop from finding self (base) -- you may want to update closest
        
        List<Activity> closestNodesInRoutingTree = routeTree.find(id, max);
        Collection<Node> closestNodesInNearSet = CollectionUtils.union(nearBucket.dumpBeforeBucket(), nearBucket.dumpAfterBucket());
        
        ArrayList<Node> res = new ArrayList<>(closestNodesInRoutingTree.size() + closestNodesInNearSet.size());
        
        Comparator<Id> idComp = new IdXorMetricComparator(id);
        Stream.concat(closestNodesInNearSet.stream(), closestNodesInRoutingTree.stream().map(x -> x.getNode()))
                .sorted((x, y) -> idComp.compare(x.getId(), y.getId()))
                .distinct()
                .limit(max)
                .forEachOrdered(res::add);
        
        return res;
    }
    
    // mark a node for eviction... if there are items available in the replacement cache for the kbucket that the node is located in, the
    // node is evicted immediately. otherwise, the node will be evicted as soon as a replacement cache item becomes available
    //
    // nodes marked for eviction WILL STILL GET RETURNED ON TOUCH(), because this is essentially signalling that we've entered desperation
    // mode... according to kademlia paper...
    //
    // When a contact fails to respond to 5 RPCs in a row, it is considered stale. If a k-bucket is not full or its replacement cache is
    // empty, Kademlia merely flags stale contacts rather than remove them. This ensures, among other things, that if a node’s own network
    // connection goes down teporarily, the node won’t completely void all of its k-buckets
    public RouterChangeSet stale(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
        InternalValidate.notMatchesBase(baseId, nodeId); 
        
        RouteTreeChangeSet routeTreeChangeSet = routeTree.stale(node);
        NearBucketChangeSet nearBucketChangeSet = synchronizeChangesFromRouteTreeToNearBucket(routeTreeChangeSet);
        
        return new RouterChangeSet(routeTreeChangeSet, nearBucketChangeSet);
    }
    
    // lock means "avoid contact" AKA avoid returning on "find" until unlocked. unlocking only happens on unlock(), not on touch()...
    //
    // according to kademlia...
    //
    // A related problem is that because Kademlia uses UDP, valid contacts will sometimes fail to respond when network packets are dropped.
    // Since packet loss often indicates network congestion, Kademlia locks unresponsive contacts and avoids sending them any further RPCs
    // for an exponentially increasing backoff interval.
    //
    // that means because we're in a "backoff period", even if we get touch()'d by that node, we still want to keep it unfindable/locked...
    // up until the point that we explictly decide to to make it findable/unlocked.
//
//COMMENTED OUT METHODS BELOW BECAUSE THEY DONT RETURN A ROUTERTREECHANGSET AND ARENT CURRENTLY BEING USED FOR ANYTHING. IF YOU'RE GOING TO
//RE-ENABLE, HAVE ROUTER DECIDE WHAT SHOULD BE LOCKED/STALE BASED ON AN UNRESPONSIVE COUNTER THAT GETS HIT WHENEVER SOMETHING IS
//UNRESPONSIVE
//    public void lock(Node node) {
//        Validate.notNull(node);
//        
//        Id nodeId = node.getId();
//        
//        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
//        InternalValidate.notMatchesBase(baseId, nodeId); 
//        
//        routeTree.lock(node); // will throw exc if node not in routetree
//        
//        nearBucket.remove(node); // remove from nearset / nearset's cache
//    }
//
//    public void unlock(Node node) {
//        Validate.notNull(node);
//        
//        Id nodeId = node.getId();
//        
//        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
//        InternalValidate.notMatchesBase(baseId, nodeId); 
//        
//        routeTree.unlock(node); // will throw exc if node not in routetree
//        
//        nearBucket.touch(node, true); // re-enter in to nearset / nearset's cache
//    }
    
    private NearBucketChangeSet synchronizeChangesFromRouteTreeToNearBucket(RouteTreeChangeSet routeTreeChangeSet) {
        KBucketChangeSet kBucketChangeSet = routeTreeChangeSet.getKBucketChangeSet();
        NearBucketChangeSet nearBucketChangeSet = new NearBucketChangeSet(NodeChangeSet.NO_CHANGE, NodeChangeSet.NO_CHANGE,
                NodeChangeSet.NO_CHANGE);
        
        for (Activity addedNode : kBucketChangeSet.getBucketChangeSet().viewAdded()) {
            // this is a new peer, so let the near bucket know
            NearBucketChangeSet tempChangeSet = nearBucket.touch(addedNode.getNode(), true);
            nearBucketChangeSet = combineNearBucketChangeSets(tempChangeSet, nearBucketChangeSet);
        }

        for (Activity removedNode : kBucketChangeSet.getBucketChangeSet().viewRemoved()) {
            // a peer was removed, so let the neat bucket know
            NearBucketChangeSet tempChangeSet = nearBucket.remove(removedNode.getNode());
            nearBucketChangeSet = combineNearBucketChangeSets(tempChangeSet, nearBucketChangeSet);
        }

        for (Activity updatedNode : kBucketChangeSet.getBucketChangeSet().viewUpdated()) {
            // this is a existing peer, so let the near bucket know
            NearBucketChangeSet tempChangeSet = nearBucket.touch(updatedNode.getNode(), true);
            nearBucketChangeSet = combineNearBucketChangeSets(tempChangeSet, nearBucketChangeSet);
        }
        
        return nearBucketChangeSet;
    }
    
    private NearBucketChangeSet combineNearBucketChangeSets(NearBucketChangeSet one, NearBucketChangeSet two) {
        return new NearBucketChangeSet(
                new NodeChangeSet(
                        CollectionUtils.union(
                                one.getBeforeBucketChangeSet().viewAdded(),
                                two.getBeforeBucketChangeSet().viewAdded()
                        ),
                        CollectionUtils.union(
                                one.getBeforeBucketChangeSet().viewRemoved(),
                                two.getBeforeBucketChangeSet().viewRemoved()
                        ),
                        CollectionUtils.union(
                                one.getBeforeBucketChangeSet().viewUpdated(),
                                two.getBeforeBucketChangeSet().viewUpdated()
                        )
                ),
                new NodeChangeSet(
                        CollectionUtils.union(
                                one.getAfterBucketChangeSet().viewAdded(),
                                two.getAfterBucketChangeSet().viewAdded()
                        ),
                        CollectionUtils.union(
                                one.getAfterBucketChangeSet().viewRemoved(),
                                two.getAfterBucketChangeSet().viewRemoved()
                        ),
                        CollectionUtils.union(
                                one.getAfterBucketChangeSet().viewUpdated(),
                                two.getAfterBucketChangeSet().viewUpdated()
                        )
                ),
                new NodeChangeSet(
                        CollectionUtils.union(
                                one.getCacheChangeSet().viewAdded(),
                                two.getCacheChangeSet().viewAdded()
                        ),
                        CollectionUtils.union(
                                one.getCacheChangeSet().viewRemoved(),
                                two.getCacheChangeSet().viewRemoved()
                        ),
                        CollectionUtils.union(
                                one.getCacheChangeSet().viewUpdated(),
                                two.getCacheChangeSet().viewUpdated()
                        )
                )
        );
    }
}
