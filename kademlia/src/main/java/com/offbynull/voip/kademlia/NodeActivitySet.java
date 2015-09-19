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
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class NodeActivitySet {
    private final Id baseId;
    
    private final TimeSet<Node> touchTimes;
    private final HashMap<Id, Activity> lookupById;
    
    private int size;
    
    public NodeActivitySet(Id baseId) {
        Validate.notNull(baseId);
        
        this.baseId = baseId;
        
        touchTimes = new TimeSet<>();
        lookupById = new HashMap<>();
    }
    
    public ActivityChangeSet touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id); // don't do this, this check provides no value for this class

        
        // See if it already exists
        Activity oldEntry = lookupById.get(id);
        if (oldEntry != null) {
            Node oldNode = oldEntry.getNode();
            
            // Check links match
            InternalValidate.matchesLink(oldNode, node);
            
            
            // Remove old timestamp
            touchTimes.remove(node);
            
            // Replace entry and insert new timestamp
            Activity activity = new Activity(node, time);
            touchTimes.insert(time, node);
            lookupById.put(id, activity);
            
            size++;
            
            // Return that node was updated
            return ActivityChangeSet.updated(activity);
        } else {
            // Insert items
            Activity activity = new Activity(node, time);
            touchTimes.insert(time, node);
            lookupById.put(id, activity);
            
            // Return that node was inserted
            return ActivityChangeSet.added(activity);
        }
    }

    public ActivityChangeSet remove(Node node) {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id); // don't do this, this check provides no value for this class
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return ActivityChangeSet.NO_CHANGE;
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        InternalValidate.matchesLink(oldNode, node);
        
        // Remove
        lookupById.remove(id);
        touchTimes.remove(node);
        
        size--;
        
        // Return that node was removed
        return ActivityChangeSet.removed(existingEntry);
    }

    public Activity get(Node node) {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        InternalValidate.matchesLength(baseId.getBitLength(), id);
//        InternalValidate.notMatchesBase(baseId, id); // don't do this, this check provides no value for this class
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return null;
        }
        
        Node existingNode = existingEntry.getNode();
        
        // Check links match
        InternalValidate.matchesLink(existingNode, node);
        
        return existingEntry;
    }
    
    @SuppressWarnings("unchecked")
    public List<Activity> getStagnantNodes(Instant maxTime) {
        Validate.notNull(maxTime);
        
        LinkedList<Activity> ret = new LinkedList<>();
        touchTimes.getBefore(maxTime, true).stream()
                .map(x -> lookupById.get(x.getId()))
                .forEach(ret::add);
        
        return new ArrayList<>(ret);
    }

    public int size() {
        return size;
    }

    @Override
    public String toString() {
        return "NodeActivitySet{" + "baseId=" + baseId + ", touchTimes=" + touchTimes +  ", lookupById=" + lookupById
                + ", size=" + size + '}';
    }
    
}
