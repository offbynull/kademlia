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
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class Router {
    private final Id baseId;
    private final RouteTree routeTree;
    
    private Instant lastTouchTime;
    
    public Router(Id baseId,
            RouteTreeBranchSpecificationSupplier branchSpecSupplier,
            RouteTreeBucketSpecificationSupplier bucketSpecSupplier) {
        Validate.notNull(baseId);
        Validate.notNull(branchSpecSupplier);
        Validate.notNull(bucketSpecSupplier);
        
        this.baseId = baseId;
        this.routeTree = new RouteTree(baseId, branchSpecSupplier, bucketSpecSupplier);
        this.lastTouchTime = Instant.MIN;
    }

    public Router(Id baseId, int branchesPerLevel, int maxNodesPerBucket, int maxCacheNodesPerBucket) {
        this(baseId,
                new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket));
    }
    
    public RouterChangeSet touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        InternalValidate.forwardTime(lastTouchTime, time); // time must be >= lastUpdatedTime
        this.lastTouchTime = time;
        
        Id nodeId = node.getId();
        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
        InternalValidate.notMatchesBase(baseId, nodeId);

        
        
        // Touch routing tree
        RouteTreeChangeSet routeTreeChangeSet = routeTree.touch(time, node);

        
        return new RouterChangeSet(routeTreeChangeSet);
    }
    
    
    public List<BitString> dumpBucketPrefixes() {
        return routeTree.dumpBucketPrefixes();
    }
    
    // DOES NOT RETURN SELF
    public List<Node> find(Id id, int max, boolean includeStale) {
        Validate.notNull(id);
        Validate.isTrue(max >= 0); // why would anyone want 0 items returned? let thru anyways
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        // do not stop from finding self (base) -- you may want to update closest
        
        List<Activity> closestNodesInRoutingTree = routeTree.find(id, max, includeStale);
        
        ArrayList<Node> res = new ArrayList<>(closestNodesInRoutingTree.size());
        
        Comparator<Id> idComp = new IdXorMetricComparator(id);
        closestNodesInRoutingTree.stream()
                .map(x -> x.getNode())
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
        
        return new RouterChangeSet(routeTreeChangeSet);
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
//    }
}
