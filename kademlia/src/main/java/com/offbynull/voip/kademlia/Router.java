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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
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

    public Router(Id baseId, int bucketsPerLevel, int maxNodesPerBucket, int maxCacheNodesPerBucket, int maxNearNodes) {
        this(baseId,
                new SimpleRouteTreeSpecificationSupplier(baseId, bucketsPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                new SimpleRouteTreeSpecificationSupplier(baseId, bucketsPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                maxNearNodes);
    }
    
    public void touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Validate.isTrue(!time.isBefore(lastTouchTime)); // time must be >= lastUpdatedTime
        this.lastTouchTime = time;
        
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());

        
        
        // Touch routing tree -- apply changes to nearbucket
        RouteTreeChangeSet routeTreeChangeSet = routeTree.touch(time, node);
        synchronizeChangesFromRouteTreeToNearSet(routeTreeChangeSet);

        // In addition to that, touch the nearbucket anyways -- if wasn't added to route tree, we may still want it because it may be nearer
        NodeChangeSet nearBucketChangeSet = nearBucket.touch(node).getBucketChangeSet();
    }
    
    public List<Node> find(Id id, int max) {
        List<Activity> closestNodesInRoutingTree = routeTree.find(id, max);
        List<Node> closestNodesInNearSet = nearBucket.dumpBucket();
        
        ArrayList<Node> res = new ArrayList<>(closestNodesInRoutingTree.size() + closestNodesInNearSet.size());
        
        Comparator<Id> idComp = new IdClosenessComparator(id);
        Stream.concat(closestNodesInNearSet.stream(), closestNodesInRoutingTree.stream().map(x -> x.getNode()))
                .sorted((x, y) -> idComp.compare(x.getId(), y.getId()))
                .distinct()
                .limit(max)
                .forEachOrdered(res::add);
        
        return res;
    }
    
    public void lock(Node node) throws LinkConflictException {
        Validate.notNull(node);
        routeTree.lock(node);
    }

    public void unlock(Node node) throws LinkConflictException {
        Validate.notNull(node);
        routeTree.unlock(node);
    }
    
    private void synchronizeChangesFromRouteTreeToNearSet(RouteTreeChangeSet routeTreeChangeSet) throws LinkConflictException {
        KBucketChangeSet kBucketChangeSet = routeTreeChangeSet.getKBucketChangeSet();
        
        for (Activity addedNode : kBucketChangeSet.getBucketChangeSet().viewAdded()) {
            nearBucket.touchPeer(addedNode.getNode()); // this is a new peer, so let the near bucket know
        }

        for (Activity removedNode : kBucketChangeSet.getBucketChangeSet().viewRemoved()) {
            nearBucket.remove(removedNode.getNode()); // a peer was removed, so let the neat bucket know
        }

        for (Activity updatedNode : kBucketChangeSet.getBucketChangeSet().viewUpdated()) {
            nearBucket.touchPeer(updatedNode.getNode()); // this is a existing peer, so let the near bucket know
        }
    }
    
//    public void setNodeProperty(Node node, Object key, Object value) throws LinkConflictException {
//        Validate.notNull(node);
//        Validate.notNull(key);
//        Validate.notNull(value);
//        
//        Id nodeId = node.getId();
//        Validate.isTrue(!nodeId.equals(baseId));
//        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
//        
//        Validate.isTrue(activitySet.get(node) != null); // activity exists
//        
//        NodeChangeSet changeSet = dataSet.put(node, key, value);
//        
//        sanityCheckChangeSet(changeSet, 0, 1, 0);// sanity check
//    }
//
//    @SuppressWarnings("unchecked")
//    public <T> T getNodeProperty(Node node, Object key) throws LinkConflictException {
//        Validate.notNull(node);
//        Validate.notNull(key);
//        
//        Id nodeId = node.getId();
//        Validate.isTrue(!nodeId.equals(baseId));
//        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
//        
//        Validate.isTrue(activitySet.get(node) != null); // activity exists
//        
//        return (T) dataSet.get(node, key);
//    }
//
//    @SuppressWarnings("unchecked")
//    public void removeNodeProperty(Node node, Object key) throws LinkConflictException {
//        Validate.notNull(node);
//        Validate.notNull(key);
//        
//        Id nodeId = node.getId();
//        Validate.isTrue(!nodeId.equals(baseId));
//        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
//        
//        Validate.isTrue(activitySet.get(node) != null); // activity exists
//        
//        dataSet.remove(node, key);
//    }

    
//    // USE THIS LATER! MAYBE!
//    private final class NodeCheckPredicate implements Predicate<Node> {
//
//        private final Predicate<RouterNodeInformation> backingPredicate;
//
//        public NodeCheckPredicate(Predicate<RouterNodeInformation> backingPredicate) {
//            Validate.notNull(backingPredicate);
//            this.backingPredicate = backingPredicate;
//        }
//        
//        @Override
//        public boolean test(Node t) {
//            Node node = t;
//            Map<Object, Object> properties;
//            Instant lastActivityTime;
//            try {
//                properties = dataSet.getAll(node);
//                lastActivityTime = activitySet.get(node).getTime();
//            } catch (LinkConflictException lce) {
//                // should never happen
//                throw new IllegalStateException(lce);
//            }
//            RoutingTreeState routingTreeState = (RoutingTreeState) properties.remove(InternalNodeProperties.ROUTINGTREE_STATE);
//            NearSetState nearSetState = (NearSetState) properties.remove(InternalNodeProperties.NEARSET_STATE);
//            
//            RouterNodeInformation testObj = new RouterNodeInformation(node, lastActivityTime, routingTreeState, nearSetState,
//                    (UnmodifiableMap<Object, Object>) UnmodifiableMap.unmodifiableMap(properties));
//            return backingPredicate.test(testObj);
//        }
//        
//    }
//    
//    public static final class RouterNodeInformation {
//        private final Node node;
//        private final Instant lastActivityTime;
//        private final RoutingTreeState routingTreeState;
//        private final NearSetState nearSetState;
//        private final UnmodifiableMap<Object, Object> properties;
//
//        public RouterNodeInformation(Node node, Instant lastActivityTime, RoutingTreeState routingTreeState, NearSetState nearSetState,
//                UnmodifiableMap<Object, Object> properties) {
//            Validate.notNull(node);
//            Validate.notNull(lastActivityTime);
//            Validate.notNull(routingTreeState);
//            Validate.notNull(nearSetState);
//            Validate.notNull(properties);
//            Validate.noNullElements(properties.keySet());
//            Validate.noNullElements(properties.values());
//            
//            this.node = node;
//            this.lastActivityTime = lastActivityTime;
//            this.routingTreeState = routingTreeState;
//            this.nearSetState = nearSetState;
//            this.properties = properties;
//        }
//
//        public Node getNode() {
//            return node;
//        }
//
//        public Instant getLastActivityTime() {
//            return lastActivityTime;
//        }
//
//        public RoutingTreeState getRoutingTreeState() {
//            return routingTreeState;
//        }
//
//        public NearSetState getNearSetState() {
//            return nearSetState;
//        }
//
//        public UnmodifiableMap<Object, Object> getProperties() {
//            return properties;
//        }
//    }
//
//    private enum InternalNodeProperties {
//        ROUTINGTREE_STATE,
//        NEARSET_STATE,
//    }
//    
//    public enum RoutingTreeState {
//        NOT_FOUND,
//        ACTIVE,
//        STALE
//    }
//    
//    public enum NearSetState {
//        NOT_FOUND,
//        ACTIVE
//    }
}
