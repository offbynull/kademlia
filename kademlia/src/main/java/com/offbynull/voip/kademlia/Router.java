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
import java.util.Arrays;
import java.util.List;
import static java.util.Locale.filter;
import java.util.Map;
import java.util.function.Predicate;
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
    
    public List<Node> getClosestNodes(Id id, int max, boolean skipPending, boolean skipStale) {
        routeTree.find(id, max, filter);
        
    }

    public List<Activity> getStagnantNodes(Instant maxTime) {
        Validate.notNull(maxTime);
        return activitySet.getStagnantNodes(maxTime);
    }

    public void stale(Node node) throws LinkConflictException {
        Id nodeId = node.getId();
        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(activitySet.get(node) != null); // activity exists
        
        routeTree.stale(node);
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
            boolean foundInRoutingTree = properties.remove(InternalNodeProperties.EXISTS_IN_ROUTINGTREE) != null;
            boolean foundInNearSet = properties.remove(InternalNodeProperties.EXISTS_IN_NEARSET) != null;
            
            RouterNodeInformation testObj = new RouterNodeInformation(node, lastActivityTime, foundInRoutingTree, foundInNearSet,
                    (UnmodifiableMap<Object, Object>) UnmodifiableMap.unmodifiableMap(properties));
            return backingPredicate.test(testObj);
        }
        
    }
    
    public static final class RouterNodeInformation {
        private final Node node;
        private final Instant lastActivityTime;
        private final boolean foundInRoutingTree;
        private final boolean foundInNearSet;
        private final boolean stale;
        private final UnmodifiableMap<Object, Object> properties;

        public RouterNodeInformation(Node node, Instant lastActivityTime, boolean foundInRoutingTree, boolean foundInNearSet,
                UnmodifiableMap<Object, Object> properties) {
            Validate.notNull(node);
            Validate.notNull(lastActivityTime);
            Validate.notNull(properties);
            Validate.noNullElements(properties.keySet());
            Validate.noNullElements(properties.values());
            
            this.node = node;
            this.lastActivityTime = lastActivityTime;
            this.foundInRoutingTree = foundInRoutingTree;
            this.foundInNearSet = foundInNearSet;
            this.properties = properties;
        }

        public Node getNode() {
            return node;
        }

        public Instant getLastActivityTime() {
            return lastActivityTime;
        }

        public boolean isFoundInRoutingTree() {
            return foundInRoutingTree;
        }

        public boolean isFoundInNearSet() {
            return foundInNearSet;
        }

        public UnmodifiableMap<Object, Object> getProperties() {
            return properties;
        }
    }
    
    private enum InternalNodeProperties {
        EXISTS_IN_ROUTINGTREE,
        EXISTS_IN_NEARSET,
    }
    
    private enum RoutingTreeState {
        ACTIVE,
        STALE
    }
}
