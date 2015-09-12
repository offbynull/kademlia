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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.apache.commons.lang3.Validate;

public final class Router {
    private static final Object UNUSED_VALUE = new Object();
    
    private final Id baseId;
    private final RouteTree routeTree;
    private final NodeNearSet nearSet;
    private final NodeActivitySet activitySet;
    private final NodeDataSet dataSet;
    
    private Instant lastUpdateTime;
    
    public Router(Id baseId, RouteTreeSpecificationSupplier specSupplier, int maxNearNodes) {
        Validate.notNull(baseId);
        Validate.notNull(specSupplier);
        Validate.isTrue(maxNearNodes >= 0);
        
        this.baseId = baseId;
        this.routeTree = RouteTree.create(baseId, specSupplier);
        this.nearSet = new NodeNearSet(baseId, maxNearNodes);
        this.activitySet = new NodeActivitySet(baseId);
        this.dataSet = new NodeDataSet(baseId);
    }

    public Router(Id baseId, int bucketsPerLevel, int maxNodesPerBucket, int maxCacheNodesPerBucket, int maxNearNodes) {
        this(baseId,
                new SimpleRouteTreeSpecificationSupplier(baseId, bucketsPerLevel, maxNodesPerBucket, maxCacheNodesPerBucket),
                maxNearNodes);
    }
    
    public void touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());

        
        
        // Touch bucket -- note that bucket can only add/update. It will never remove when you touch.
        ActivityChangeSet bucketChangeSet = routeTree.touch(time, node).getBucketChangeSet();
        if (!bucketChangeSet.viewAdded().isEmpty()) {
            // Node was added to bucket
            ActivityChangeSet activityChangeSet = activitySet.touch(time, node); // touch activityset with this node
            NodeChangeSet dataChangeSet = dataSet.put(node, InternalNodeProperties.EXISTS_IN_ROUTINGTREE, UNUSED_VALUE);
            
            // This is a new entry -- sanity check that it was added in to all sets and nothing was updated/removed in the process.
            sanityCheckChangeSet(bucketChangeSet, 1, 0, 0);
            sanityCheckChangeSet(activityChangeSet, 1, 0, 0);
            sanityCheckChangeSet(dataChangeSet, 1, 0, 0);
        } else if (!bucketChangeSet.viewUpdated().isEmpty()) {
            // Node was updated in bucket
            ActivityChangeSet activityChangeSet = activitySet.touch(time, node);
            
            // If node was stale, it means it no longer is stale now
            dataSet.remove(node, InternalNodeProperties.STALE_IN_ROUTINGTREE);
            
            // This is an update to an existing entry -- sanity check that it was updated in all sets and nothing was added/removed in the
            // process. Also validate that node is already flagged as being in the routing tree.
            sanityCheckChangeSet(bucketChangeSet, 0, 1, 0);
            sanityCheckChangeSet(activityChangeSet, 0, 1, 0);
            Validate.validState(dataSet.get(node, InternalNodeProperties.EXISTS_IN_ROUTINGTREE) == UNUSED_VALUE);
        } else {
            // This should never happen -- sanity check that the bucket doesn't remove anything as the result of a touch.
            throw new IllegalStateException();
        }
        
        
        
        
        
        
        // Touch nearset -- note that nearset can only add, update, or replace (add and remove at the same time) when you touch.
        NodeChangeSet nearChangeSet = nearSet.touch(node);
        if (!nearChangeSet.viewAdded().isEmpty()) {
            // Node was added to the near set
            ActivityChangeSet activityChangeSet = activitySet.touch(time, node);
            dataSet.put(node, InternalNodeProperties.EXISTS_IN_NEARSET, UNUSED_VALUE);

            // This is a new entry -- sanity check that the only other thing that can happen is that a node could be evicted from nearset
            // due to the add.
            sanityCheckChangeSet(nearChangeSet, 1, 0, new int[] {0, 1});
            sanityCheckChangeSet(activityChangeSet, 1, 0, 0);
            
            // Since node was added to nearset, it may have caused another node to be removed. If that happens, remove the nearset marker on
            // the removed node.
            if (!nearChangeSet.viewRemoved().isEmpty()) {
                Node removedNode = nearChangeSet.viewRemoved().get(0);
                NodeChangeSet removeDataChangeSet = dataSet.remove(removedNode, InternalNodeProperties.EXISTS_IN_NEARSET);
                
                // This may cause the node to be removed from teh dataset -- sanity check to make sure at most 1 nodes removed.
                sanityCheckChangeSet(removeDataChangeSet, 0, 0, new int[]{0, 1}); // 0 or 1 because there may be more data
               
                // If that removed node is also no longer in the routing table, remove it from the activityset entirely
                if (dataSet.get(removedNode, InternalNodeProperties.EXISTS_IN_ROUTINGTREE) == null) {
                    NodeChangeSet removeAllDataChangeSet = dataSet.removeAll(removedNode);
                    ActivityChangeSet removeActivityChangeSet = activitySet.remove(removedNode);
                
                    // The removed node is being removed entirely -- sanity check that to make sure only removals are happening.
                    sanityCheckChangeSet(removeAllDataChangeSet, 0, 0, new int[]{0, 1});  // 0 or 1 because keys may been cleared before
                    sanityCheckChangeSet(removeActivityChangeSet, 0, 0, 1);
                }
            }
        } else if (!nearChangeSet.viewUpdated().isEmpty()) {
            // Node was already in bucket, and was updated
            ActivityChangeSet activityChangeSet = activitySet.touch(time, node);
            
            // This is an update to an existing entry -- sanity check that it was updated in all sets and nothing was added/removed in the
            // process. Also validate that node is already flagged as being in the near set.
            sanityCheckChangeSet(nearChangeSet, 0, 1, 0);
            sanityCheckChangeSet(activityChangeSet, 0, 1, 0);
            Validate.validState(dataSet.get(node, InternalNodeProperties.EXISTS_IN_NEARSET) != null);
        } else {
            // This should never happen -- sanity check that the nearset never removes anything without an add. A remove must be accompanied
            // by an add if its from a touch.
            throw new IllegalStateException();
        }
    }
    
    public List<Node> getClosestNodes(Id id, int max, Predicate<RouterNodeInformation> filter) {
        Predicate<Node> nodeFilter = new NodeCheckPredicate(filter);
        List<Node> closestNodesInRoutingTree = routeTree.find(id, max, nodeFilter);
        List<Node> closestNodes = nearSet.dump();
        
        List<Node> res = new ArrayList<>(closestNodesInRoutingTree.size() + closestNodes.size());
        
        Comparator<Id> idComp = new IdClosenessComparator(id);
        Stream.concat(closestNodes.stream().filter(nodeFilter), closestNodesInRoutingTree.stream())
                .sorted((x,y) -> -idComp.compare(x.getId(), y.getId()))
                .limit(max)
                .forEachOrdered(res::add);
        
        return res;
    }

    public List<Activity> getStagnantNodes(Instant maxTime, Predicate<RouterNodeInformation> filter) {
        Validate.notNull(maxTime);
        return activitySet.getStagnantNodes(maxTime, filter);
    }

    public void unresponsive(Node node) throws LinkConflictException {
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(activitySet.get(node) != null); // activity exists
        
        if (dataSet.get(node, InternalNodeProperties.ROUTINGTREE_STATE) != RoutingTreeState.NOT_FOUND) {
            KBucketChangeSet res = routeTree.stale(node);
            if (res.getBucketChangeSet().viewRemoved().isEmpty()) {
                // Nothing was removed, so that means that the node wasn't replaced by a cache item (because there are no cache items
                // available). Instead it was marked as stale and is slated for a removal whenver a new node hits that bucket (or if it gets
                // touched again).
                dataSet.put(node, InternalNodeProperties.ROUTINGTREE_STATE, RoutingTreeState.STALE);
            } else {
                // Node was removed and a new node (from the cache) was put in its place.
                dataSet.removeAll(node);
                
                // TODO: FIND EARLIEST BUCKET NODE AND USE TO REPLACE HERE.
            }
        }
        
        if (dataSet.get(node, InternalNodeProperties.NEARSET_STATE) != NearSetState.NOT_FOUND) {
            
        }
    }
    
    public void setNodeProperty(Node node, Object key, Object value) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        Validate.notNull(value);
        
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(activitySet.get(node) != null); // activity exists
        
        NodeChangeSet changeSet = dataSet.put(node, key, value);
        
        sanityCheckChangeSet(changeSet, 0, 1, 0);// sanity check
    }

    @SuppressWarnings("unchecked")
    public <T> T getNodeProperty(Node node, Object key) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(activitySet.get(node) != null); // activity exists
        
        return (T) dataSet.get(node, key);
    }

    @SuppressWarnings("unchecked")
    public void removeNodeProperty(Node node, Object key) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(activitySet.get(node) != null); // activity exists
        
        dataSet.remove(node, key);
    }
    
    private void sanityCheckChangeSet(NodeChangeSet set, int expectedAddedCount, int expectedUpdatedCount, int expectedRemovedCount) {
        Validate.validState(set.viewAdded().size() == expectedAddedCount);
        Validate.validState(set.viewUpdated().size() == expectedUpdatedCount);
        Validate.validState(set.viewRemoved().size() == expectedRemovedCount);
    }

    private void sanityCheckChangeSet(NodeChangeSet set, int expectedAddedCount, int expectedUpdatedCount,
            int[] allowedRemovedCounts) {
        Validate.validState(set.viewAdded().size() == expectedAddedCount);
        Validate.validState(set.viewUpdated().size() == expectedUpdatedCount);
        Validate.validState(Arrays.stream(allowedRemovedCounts).anyMatch(x -> x == set.viewRemoved().size()));
    }

    private void sanityCheckChangeSet(ActivityChangeSet set, int expectedAddedCount, int expectedUpdatedCount, int expectedRemovedCount) {
        Validate.validState(set.viewAdded().size() == expectedAddedCount);
        Validate.validState(set.viewUpdated().size() == expectedUpdatedCount);
        Validate.validState(set.viewRemoved().size() == expectedRemovedCount);
    }
    
    private final class NodeCheckPredicate implements Predicate<Node> {

        private final Predicate<RouterNodeInformation> backingPredicate;

        public NodeCheckPredicate(Predicate<RouterNodeInformation> backingPredicate) {
            Validate.notNull(backingPredicate);
            this.backingPredicate = backingPredicate;
        }
        
        @Override
        public boolean test(Node t) {
            Node node = t;
            Map<Object, Object> properties;
            Instant lastActivityTime;
            try {
                properties = dataSet.getAll(node);
                lastActivityTime = activitySet.get(node).getTime();
            } catch (LinkConflictException lce) {
                // should never happen
                throw new IllegalStateException(lce);
            }
            RoutingTreeState routingTreeState = (RoutingTreeState) properties.remove(InternalNodeProperties.ROUTINGTREE_STATE);
            NearSetState nearSetState = (NearSetState) properties.remove(InternalNodeProperties.NEARSET_STATE);
            
            RouterNodeInformation testObj = new RouterNodeInformation(node, lastActivityTime, routingTreeState, nearSetState,
                    (UnmodifiableMap<Object, Object>) UnmodifiableMap.unmodifiableMap(properties));
            return backingPredicate.test(testObj);
        }
        
    }
    
    public static final class RouterNodeInformation {
        private final Node node;
        private final Instant lastActivityTime;
        private final RoutingTreeState routingTreeState;
        private final NearSetState nearSetState;
        private final UnmodifiableMap<Object, Object> properties;

        public RouterNodeInformation(Node node, Instant lastActivityTime, RoutingTreeState routingTreeState, NearSetState nearSetState,
                UnmodifiableMap<Object, Object> properties) {
            Validate.notNull(node);
            Validate.notNull(lastActivityTime);
            Validate.notNull(routingTreeState);
            Validate.notNull(nearSetState);
            Validate.notNull(properties);
            Validate.noNullElements(properties.keySet());
            Validate.noNullElements(properties.values());
            
            this.node = node;
            this.lastActivityTime = lastActivityTime;
            this.routingTreeState = routingTreeState;
            this.nearSetState = nearSetState;
            this.properties = properties;
        }

        public Node getNode() {
            return node;
        }

        public Instant getLastActivityTime() {
            return lastActivityTime;
        }

        public RoutingTreeState getRoutingTreeState() {
            return routingTreeState;
        }

        public NearSetState getNearSetState() {
            return nearSetState;
        }

        public UnmodifiableMap<Object, Object> getProperties() {
            return properties;
        }
    }

    private enum InternalNodeProperties {
        ROUTINGTREE_STATE,
        NEARSET_STATE,
    }
    
    public enum RoutingTreeState {
        NOT_FOUND,
        ACTIVE,
        STALE
    }
    
    public enum NearSetState {
        NOT_FOUND,
        ACTIVE
    }
}
