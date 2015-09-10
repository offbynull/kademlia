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
import java.util.LinkedList;
import java.util.List;
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

    public NodeChangeSet touch(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        if (maxSize == 0) {
            return NodeChangeSet.NO_CHANGE;
        }
        
        List<Node> added = new ArrayList<>(1);
        List<Node> removed = new ArrayList<>(1);
        
        Node existingNode;
        if ((existingNode = nodes.get(nodeId)) != null) {
            if (!existingNode.equals(node)) {
                // if ID exists but link for ID is different, ignore
                throw new LinkConflictException(existingNode);
            }
            
            return NodeChangeSet.NO_CHANGE; // already exists
        }
        
        added.add(node);
        nodes.put(nodeId, node);
        if (nodes.size() > maxSize) {
            Node oldNode = nodes.pollFirstEntry().getValue(); // remove first node (farthest) so we don't exceed maxSize
            removed.add(oldNode);
        }
        
        return new NodeChangeSet(added, removed, emptyList());
    }
    
    public NodeChangeSet remove(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Node foundNode = nodes.get(nodeId);
        if (foundNode == null) {
            return NodeChangeSet.NO_CHANGE;
        }

        Id foundId = foundNode.getId();
        String foundLink = foundNode.getLink();

        Validate.validState(nodeId.equals(foundId)); // should never happen -- just in case
        if (!foundLink.equals(nodeLink)) {
            // if ID exists but link for ID is different
            throw new LinkConflictException(foundNode);
        }

        // remove
        nodes.remove(nodeId);
        return NodeChangeSet.removed(foundNode);
    }
    
    public NodeChangeSet resize(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        int discardCount = this.maxSize - maxSize;
        
        List<Node> removed = new LinkedList<>();
        for (int i = 0; i < discardCount; i++) {
            Node removedEntry = nodes.pollFirstEntry().getValue(); // remove largest
            removed.add(removedEntry);
        }
        
        this.maxSize = maxSize;
        
        return NodeChangeSet.removed(removed);
    }
    
    public List<Node> dump() {
        return new ArrayList<>(nodes.values());
    }
    
    public int size() {
        return nodes.size();
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    @Override
    public String toString() {
        return "NearSet{" + "baseId=" + baseId + ", entries=" + nodes + ", maxSize=" + maxSize + '}';
    }
    
}
