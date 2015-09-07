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

import java.time.Instant;
import java.util.ArrayList;
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

    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        if (maxSize == 0) {
            return TouchResult.IGNORED;
        }
        
        Entry existingEntry;
        if ((existingEntry = entries.get(nodeId)) != null) {
            if (!existingEntry.getNode().equals(node)) {
                // if ID exists but link for ID is different, ignore
                return TouchResult.CONFLICTED;
            }
            
            entries.put(nodeId, new Entry(node, time));
            return TouchResult.UPDATED;
        }
        
        entries.put(nodeId, new Entry(node, time));
        if (entries.size() > maxSize) {
            entries.pollFirstEntry(); // remove first entry so we don't exceed maxSize
        }
        
        return TouchResult.UPDATED;
    }
    
    public RemoveResult remove(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Entry entry = entries.get(nodeId);
        if (entry == null) {
            return RemoveResult.NOT_FOUND;
        }

        Id entryId = entry.getNode().getId();
        String entryLink = entry.getNode().getLink();

        Validate.validState(nodeId.equals(entryId)); // should never happen -- just in case
        if (!entryLink.equals(nodeLink)) {
            // if ID exists but link for ID is different
            return RemoveResult.CONFLICTED;
        }

        // remove
        entries.remove(nodeId);
        return RemoveResult.REMOVED;
    }
    
    public void resize(int maxSize) {
        Validate.isTrue(maxSize >= 1);
        
        int discardCount = this.maxSize - maxSize;
        
        for (int i = 0; i < discardCount; i++) {
            entries.pollFirstEntry(); // remove largest
        }
        
        this.maxSize = maxSize;
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
    
    public enum RemoveResult {
        REMOVED, // removed
        NOT_FOUND, // id to replace couldn't be found
        CONFLICTED // entry with same id already existed, but link is different, so ignoring
    }
    
    public enum TouchResult {
        UPDATED, // entry inserted or updated
        IGNORED, // set is full and entry's time was too far out to get replace existing (too far in to the past to replace existing)
        CONFLICTED, // entry with same id already existed, but link is different, so ignoring
    }

    @Override
    public String toString() {
        return "NearSet{" + "baseId=" + baseId + ", entries=" + entries + ", maxSize=" + maxSize + '}';
    }
    
}
