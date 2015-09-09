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

public final class ActivitySet {
    private final Id baseId;
    
    private final TreeMap<Instant, HashSet<Node>> touchTimes;
    private final MultiValueMap<Instant, Node> touchTimesDecorator;
    private final HashMap<Id, Activity> lookupById;
    private final HashSet<Id> ignoreSet;
    
    private int size;
    
    public ActivitySet(Id baseId) {
        Validate.notNull(baseId);
        
        this.baseId = baseId;
        
        touchTimes = new TreeMap<>();
        touchTimesDecorator = MultiValueMap.multiValueMap(touchTimes, () -> new HashSet<>());
        lookupById = new HashMap<>();
        ignoreSet = new HashSet<>();
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
        ignoreSet.remove(id);
        
        size--;
        
        // Return that node was removed
        return ActivityChangeSet.removed(existingEntry);
    }
    
    public void ignore(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return;
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        if (!oldNode.equals(node)) {
            throw new LinkConflictException(oldNode);
        }
        
        // Add to ignore set
        ignoreSet.add(id);
    }
    
    public void unignore(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Activity existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return;
        }
        
        Node oldNode = existingEntry.getNode();
        
        // Check links match
        if (!oldNode.equals(node)) {
            throw new LinkConflictException(oldNode);
        }
        
        // Add to ignore set
        ignoreSet.add(id);
    }
    
    @SuppressWarnings("unchecked")
    public List<Activity> getNodes(Instant maxTime) {
        Validate.notNull(maxTime);
        
        Map<Instant, HashSet<Node>> subMap = touchTimes.headMap(maxTime, true);
        
        LinkedList<Activity> ret = new LinkedList<>();
        subMap.entrySet().stream()
                .flatMap(x -> x.getValue().stream())
                .filter(x -> !ignoreSet.contains(x.getId()))
                .map(x -> lookupById.get(x.getId()))
                .forEach(x -> ret.add(x));
        
        return new ArrayList<>(ret);
    }

    public int size() {
        return size;
    }
    
}
