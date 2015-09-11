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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class NodeDataSet {
    private final Id baseId;
    private final Map<Id, DataHolder> nodes;
    
    public NodeDataSet(Id baseId) {
        Validate.notNull(baseId);
        
        this.baseId = baseId;
        this.nodes = new HashMap<>();
    }
    
    public NodeChangeSet put(Node node, Object key, Object value) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        Validate.notNull(value);

        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        NodeChangeSet ret;
        DataHolder dataHolder = nodes.get(nodeId);
        if (dataHolder == null) {
            dataHolder = new DataHolder(node);
            nodes.put(nodeId, dataHolder);
            ret = NodeChangeSet.added(dataHolder.getNode());
        } else {
            validateNode(node, dataHolder);
            ret = NodeChangeSet.NO_CHANGE;
        }

        dataHolder.put(key, value);
        
        return ret;
    }

    public Object get(Node node, Object key) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        DataHolder dataHolder = nodes.get(nodeId);
        if (dataHolder == null) {
            return null;
        }
        
        validateNode(node, dataHolder);
        return dataHolder.get(key);
    }

    public Map<Object, Object> getAll(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        DataHolder dataHolder = nodes.get(nodeId);
        if (dataHolder == null) {
            return null;
        }
        
        validateNode(node, dataHolder);
        return dataHolder.getAll();
    }

    public NodeChangeSet remove(Node node, Object key) throws LinkConflictException {
        Validate.notNull(node);
        Validate.notNull(key);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        DataHolder dataHolder = nodes.get(nodeId);
        
        // If you don't have the node, remove
        if (dataHolder == null) {
            return NodeChangeSet.NO_CHANGE;
        }
        
        validateNode(node, dataHolder);
        
        // If you do have the node, try to remove the value associated with this key.
        dataHolder.remove(key);
        
        // If you have no more data after removal of this key, remove the node.
        if (dataHolder.isEmpty()) {
            Node removedNode = dataHolder.getNode();
            nodes.remove(nodeId);
            return NodeChangeSet.removed(removedNode);
        }
        
        // You have more data left, which means the node is still in this set, so return a no change.
        return NodeChangeSet.NO_CHANGE;
    }

    public NodeChangeSet removeAll(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        DataHolder dataHolder = nodes.remove(nodeId);
        
        if (dataHolder == null) {
            return NodeChangeSet.NO_CHANGE;
        }
        
        validateNode(node, dataHolder);
        
        Node removedNode = dataHolder.getNode();
        return NodeChangeSet.removed(removedNode);
    }
    
    private void validateNode(Node incomingNode, DataHolder dataHolder) throws LinkConflictException {
        Validate.notNull(incomingNode);
        Validate.notNull(dataHolder);
        
        Node existingNode = dataHolder.getNode();
        Validate.isTrue(incomingNode.getId().equals(existingNode.getId()));
        if (!dataHolder.getNode().getLink().equals(incomingNode.getLink())) {
            throw new LinkConflictException(existingNode);
        }
    }

    @Override
    public String toString() {
        return "NodeDataSet{" + "baseId=" + baseId + ", nodes=" + nodes + '}';
    }

    
    
    private static final class DataHolder {
        private Node node;
        private final Map<Object, Object> data;

        public DataHolder(Node node) {
            Validate.notNull(node);
            this.node = node;
            this.data = new HashMap<>();
        }

        public void put(Object key, Object val) {
            Validate.notNull(key);
            Validate.notNull(val);
            data.put(key, val);
        }

        public void remove(Object key) {
            Validate.notNull(key);
            data.remove(key);
        }

        public Object get(Object key) {
            Validate.notNull(key);
            return data.get(key);
        }

        public Map<Object, Object> getAll() {
            return new HashMap<>(data);
        }
        
        public boolean isEmpty() {
            return data.isEmpty();
        }
        
        public Node getNode() {
            return node;
        }
    }
}
