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

import java.util.ArrayList;
import static java.util.Collections.emptyList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.apache.commons.lang3.Validate;

public final class NodeNearSet {
    private final Id baseId;
    private final TreeMap<Id, Node> nodes;
    
    private int maxSize;

    public NodeNearSet(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0);
        
        this.baseId = baseId;
        this.maxSize = maxSize;
        
        this.nodes = new TreeMap<>(new IdClosenessComparator(baseId));
    }

    public NodeChangeSet touch(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
//        InternalValidate.notMatchesBase(baseId, nodeId); // don't do this, this check provides no value for this class
        
        if (maxSize == 0) {
            return NodeChangeSet.NO_CHANGE;
        }
        
        List<Node> added = new ArrayList<>(1);
        List<Node> removed = new ArrayList<>(1);
        
        Node existingNode;
        if ((existingNode = nodes.get(nodeId)) != null) {
            InternalValidate.matchesLink(existingNode, node);
            
            return NodeChangeSet.updated(node); // already exists -- show as being updated to indicate that it already exists
        }
        
        added.add(node);
        nodes.put(nodeId, node);
        if (nodes.size() > maxSize) {
            Node oldNode = nodes.pollLastEntry().getValue(); // remove first node (farthest) so we don't exceed maxSize
            removed.add(oldNode);
            
            // if the node we evicted it the one that we added, that means no change has occured
            if (node.equals(oldNode)) {
                return NodeChangeSet.NO_CHANGE;
            }
        }
        
        return new NodeChangeSet(added, removed, emptyList());
    }
    
    public NodeChangeSet remove(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), nodeId);
//        InternalValidate.notMatchesBase(baseId, nodeId); // don't do this, this check provides no value for this class
        
        Node foundNode = nodes.get(nodeId);
        if (foundNode == null) {
            return NodeChangeSet.NO_CHANGE;
        }

        Id foundId = foundNode.getId();

        Validate.validState(nodeId.equals(foundId)); // should never happen -- just in case
        InternalValidate.matchesLink(foundNode, node);

        // remove
        nodes.remove(nodeId);
        return NodeChangeSet.removed(foundNode);
    }
    
    public NodeChangeSet resize(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        int discardCount = this.maxSize - maxSize;
        
        LinkedList<Node> removed = new LinkedList<>();
        for (int i = 0; i < discardCount; i++) {
            Node removedEntry = nodes.pollLastEntry().getValue(); // remove largest
            removed.addFirst(removedEntry);
        }
        
        this.maxSize = maxSize;
        
        return NodeChangeSet.removed(removed);
    }
    
    public List<Node> dump() {
        return new ArrayList<>(nodes.values());
    }

    public List<Node> dumpNearestBefore(Id id, int max) { // dumps up to max nodes < id, starting from nearest in set (EXCLUSIVE)
        Validate.notNull(id); // perfectly okay for id to equal baseId;
        Validate.isTrue(max >= 0);
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        
        NavigableMap<Id, Node> partialMap = nodes.headMap(id, false);
        
        int remaining = Math.min(max, nodes.size());
        List<Node> ret = new ArrayList<>(remaining);
        
        for (Entry<Id, Node> entry : partialMap.entrySet()) {
            if (remaining == 0) {
                break;
            }
            ret.add(entry.getValue());
            remaining--;
        }
        
        return ret;
    }

    public List<Node> dumpNearestAfter(Id id, int max) { // dumps up to max nodes > id, starting from nearest to id (EXCLUSIVE)
        Validate.notNull(id); // perfectly okay for id to equal baseId;
        Validate.isTrue(max >= 0);
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
        
        NavigableMap<Id, Node> partialMap = nodes.tailMap(id, false);
        
        int remaining = Math.min(max, nodes.size());
        List<Node> ret = new ArrayList<>(remaining);
        
        for (Entry<Id, Node> entry : partialMap.entrySet()) {
            if (remaining == 0) {
                break;
            }
            ret.add(entry.getValue());
            remaining--;
        }
        
        return ret;
    }
    
    public int size() {
        return nodes.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public String toString() {
        return "NodeNearSet{" + "baseId=" + baseId + ", nodes=" + nodes + ", maxSize=" + maxSize + '}';
    }

}