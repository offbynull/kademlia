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
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.commons.lang3.Validate;

/**
 * Cache for a Kademlia k-bucket. The cache will hold up to a certain number of nodes
 * @author Kasra Faghihi
 */
public final class Cache {
    private final Id baseId;
    private final int commonPrefixSize; // For something to be allowed in this cache, it needs to share a common prefix of this many bits
                                        // with baseId field

    private final LinkedList<Entry> entries;
    private final int maxCacheSize;
    
    private Instant lastUpdateTime;

    public Cache(Id baseId, int commonPrefixSize, int maxCacheSize) {
        Validate.notNull(baseId);
        Validate.isTrue(commonPrefixSize >= 0);
        Validate.isTrue(commonPrefixSize <= baseId.getBitLength());
        Validate.isTrue(maxCacheSize > 0);
        
        this.baseId = baseId;
        this.commonPrefixSize = commonPrefixSize;
        this.maxCacheSize = maxCacheSize;

        this.entries = new LinkedList<>();
        
        lastUpdateTime = Instant.MIN;
    }
    
    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Validate.isTrue(baseId.getBitLength() == nodeId.getBitLength());
        Validate.isTrue(baseId.getSharedPrefixLength(nodeId) == commonPrefixSize);
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime

        // If not true, this essentially means that this.id and id are equal, so what's the point of having a cache? Validate here?
        // Validate.isTrue(commonPrefixBitCount < id.getBitLength());
        
        lastUpdateTime = time;
        
        // Update if already exists
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            
            Id entryId = entry.getNode().getId();
            String entryLink = entry.getNode().getLink();
            
            if (entryId.equals(nodeId)) {
                if (!entryLink.equals(nodeLink)) {
                    // if ID exists but link for ID is different, ignore
                    return TouchResult.IGNORED;
                }
                
                // remove and add to tail (most recently-seen)
                it.remove();
                entry.setLastSeenTime(time);
                entries.addLast(entry);
                return TouchResult.UPDATED;
            }
        }
        
        if (entries.size() == maxCacheSize) {
            entries.removeFirst();
        }
        
        // Add
        Entry entry = new Entry(node, time);
        entries.addLast(entry);
        return TouchResult.UPDATED;
    }
    
    public enum TouchResult {
        INSERTED, // inserted as latest entry
        UPDATED, // entry moved to latest entry
        IGNORED, // entry with same id already existed, but link is different, so ignoring
    }
}
