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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

public final class NodeActivitySet {
    private final Id baseId;
    
    private final TreeMap<Instant, HashSet<Node>> touchTimes;
    private final MultiValueMap<Instant, Node> touchTimesDecorator;
    private final HashMap<Id, Activity> lookupById;
    private final HashSet<Id> pending;
    
    private int size;
    
    public NodeActivitySet(Id baseId) {
        Validate.notNull(baseId);
        
        this.baseId = baseId;
        
        touchTimes = new TreeMap<>();
        touchTimesDecorator = MultiValueMap.multiValueMap(touchTimes, () -> new HashSet<>());
        lookupById = new HashMap<>();
        pending = new HashSet<>();
    }
    
    public ActivityChangeSet touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!id.equals(baseId));

        
        // See if it already exists
        Activity oldEntry = lookupById.get(id);
        if (oldEntry != null) {
            Node oldNode = oldEntry.getNode();
            
            // Check links match
            if (!oldNode.equals(node)) {
                throw new LinkConflictException(oldNode);
            }
            
            
            // Check not pending
            Validate.validState(!pending.contains(id));
            
            // Remove old timestamp
            Instant oldTimestamp = oldEntry.getTime();
            touchTimesDecorator.removeMapping(oldTimestamp, node);
            
            // Replace entry and insert new timestamp
            Activity activity = new Activity(node, time);
            touchTimesDecorator.put(time, node);
            lookupById.put(id, activity);
            
            size++;
            
            // Return that node was updated
            return ActivityChangeSet.updated(activity);
        } else {
            // Insert items
            Activity activity = new Activity(node, time);
            touchTimesDecorator.put(time, node);
            lookupById.put(id, activity);
            
            // Return that node was inserted
            return ActivityChangeSet.added(activity);
        }
    }

    public ActivityChangeSet remove(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return ActivityChangeSet.NO_CHANGE;
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        if (!oldNode.equals(node)) {
            throw new LinkConflictException(oldNode);
        }
        
        // Remove
        lookupById.remove(id);
        touchTimesDecorator.removeMapping(existingEntry.getTime(), node);
        pending.remove(id);
        
        size--;
        
        // Return that node was removed
        return ActivityChangeSet.removed(existingEntry);
    }
    
    public void pending(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            throw new IllegalArgumentException();
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        if (!oldNode.equals(node)) {
            throw new LinkConflictException(oldNode);
        }
        
        // Add to ignore set
        boolean added = pending.add(id);
        Validate.isTrue(added);
    }
    
    public void idle(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            throw new IllegalArgumentException();
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        if (!oldNode.equals(node)) {
            throw new LinkConflictException(oldNode);
        }
        
        // Add to ignore set
        boolean removed = pending.remove(id);
        Validate.isTrue(removed);
    }
    
    @SuppressWarnings("unchecked")
    public List<Activity> getNodes(Instant maxTime) {
        Validate.notNull(maxTime);
        
        Map<Instant, HashSet<Node>> subMap = touchTimes.headMap(maxTime, true);
        
        LinkedList<Activity> ret = new LinkedList<>();
        subMap.entrySet().stream()
                .flatMap(x -> x.getValue().stream())
                .filter(x -> !pending.contains(x.getId()))
                .map(x -> lookupById.get(x.getId()))
                .forEach(x -> ret.add(x));
        
        return new ArrayList<>(ret);
    }

    public int size() {
        return size;
    }
    
}
