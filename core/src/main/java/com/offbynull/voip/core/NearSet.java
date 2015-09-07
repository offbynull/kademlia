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
package com.offbynull.voip.core;

import com.offbynull.voip.core.ChangeSet.UpdatedEntry;
import java.time.Instant;
import java.util.ArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.lang3.Validate;

public final class NearSet {
    private final Id baseId;
    private final TreeMap<Id, Entry> entries;
    
    private int maxSize;

    public NearSet(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0);
        
        this.baseId = baseId;
        this.maxSize = maxSize;
        
        this.entries = new TreeMap<>(new IdClosenessComparator(baseId));
    }

    public ChangeSet touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        if (maxSize == 0) {
            return ChangeSet.NO_CHANGE;
        }
        
        List<Entry> added = new ArrayList<>(1);
        List<Entry> removed = new ArrayList<>(1);
        List<UpdatedEntry> updated = new ArrayList<>(1);
        
        Entry newEntry = new Entry(node, time);
        Entry existingEntry;
        if ((existingEntry = entries.get(nodeId)) != null) {
            if (!existingEntry.getNode().equals(node)) {
                // if ID exists but link for ID is different, ignore
                return ChangeSet.NO_CHANGE;
            }
            
            updated.add(new UpdatedEntry(node, existingEntry.getLastSeenTime(), newEntry.getLastSeenTime()));
            entries.put(nodeId, newEntry);
            return new ChangeSet(added, removed, updated);
        }
        
        added.add(newEntry);
        entries.put(nodeId, new Entry(node, time));
        if (entries.size() > maxSize) {
            Entry oldEntry = entries.pollFirstEntry().getValue(); // remove first entry so we don't exceed maxSize
            removed.add(oldEntry);
        }
        
        return new ChangeSet(added, removed, updated);
    }
    
    public ChangeSet remove(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Entry entry = entries.get(nodeId);
        if (entry == null) {
            return ChangeSet.NO_CHANGE;
        }

        Id entryId = entry.getNode().getId();
        String entryLink = entry.getNode().getLink();

        Validate.validState(nodeId.equals(entryId)); // should never happen -- just in case
        if (!entryLink.equals(nodeLink)) {
            // if ID exists but link for ID is different
            return ChangeSet.NO_CHANGE;
        }

        // remove
        entries.remove(nodeId);
        return ChangeSet.removed(entry);
    }
    
    public ChangeSet resize(int maxSize) {
        Validate.isTrue(maxSize >= 1);
        
        int discardCount = this.maxSize - maxSize;
        
        List<Entry> removed = new LinkedList<>();
        for (int i = 0; i < discardCount; i++) {
            Entry removedEntry = entries.pollFirstEntry().getValue(); // remove largest
            removed.add(removedEntry);
        }
        
        this.maxSize = maxSize;
        
        return ChangeSet.removed(removed);
    }
    
    public List<Entry> dump() {
        return new ArrayList<>(entries.values());
    }
    
    public int size() {
        return entries.size();
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    @Override
    public String toString() {
        return "NearSet{" + "baseId=" + baseId + ", entries=" + entries + ", maxSize=" + maxSize + '}';
    }
    
}
