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
    private final HashMap<Id, Entry> lookupById;
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
    
    public NodeChangeSet touch(Instant time, Node node) throws LinkConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!id.equals(baseId));

        
        // See if it already exists
        Entry oldEntry = lookupById.get(id);
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
            touchTimesDecorator.put(time, node);
            lookupById.put(id, new Entry(node, time));
            
            size++;
            
            // Return that node was updated
            return NodeChangeSet.updated(node);
        } else {
            // Insert items
            touchTimesDecorator.put(time, node);
            lookupById.put(id, new Entry(node, time));
            
            // Return that node was inserted
            return NodeChangeSet.added(node);
        }
    }

    public NodeChangeSet remove(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Entry existingEntry = lookupById.get(id);
        if (existingEntry == null) {
            return NodeChangeSet.NO_CHANGE;
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
        return NodeChangeSet.removed(node);
    }
    
    public void ignore(Node node) throws LinkConflictException {
        Validate.notNull(node);
        
        Id id = node.getId();
        
        Entry existingEntry = lookupById.get(id);
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
        
        Entry existingEntry = lookupById.get(id);
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
    public List<Node> getNodes(Instant maxTime) {
        Validate.notNull(maxTime);
        
        Map<Instant, HashSet<Node>> subMap = touchTimes.headMap(maxTime, true);
        
        LinkedList<Node> ret = new LinkedList<>();
        subMap.entrySet().stream()
                .flatMap(x -> x.getValue().stream())
                .filter(x -> !ignoreSet.contains(x.getId()))
                .forEach(x -> ret.add(x));
        
        return new ArrayList<>(ret);
    }

    public int size() {
        return size;
    }
    
}
