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
    private final TreeMap<Id, Node> entries;
    
    private int maxSize;
    
    private Instant lastUpdateTime;

    public NearSet(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0);
        
        this.baseId = baseId;
        this.maxSize = maxSize;
        
        this.entries = new TreeMap<>(new IdClosenessComparator(baseId));
        
        lastUpdateTime = Instant.MIN;
    }

    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        
        Node existingNode;
        if ((existingNode = entries.get(nodeId)) != null) {
            if (!existingNode.equals(node)) {
                // if ID exists but link for ID is different, ignore
                return TouchResult.IGNORED;
            }
            
            lastUpdateTime = time;
            return TouchResult.UPDATED;
        }
        
        entries.put(nodeId, node);
        lastUpdateTime = time;
        
        if (entries.size() <= maxSize) {
            return TouchResult.INSERTED;
        } else {
            entries.pollFirstEntry(); // remove first entry so we don't exceed maxSize
            return TouchResult.REPLACED;
        }
    }
    
    public void resize(int maxSize) {
        Validate.isTrue(maxSize >= 1);
        
        int discardCount = this.maxSize - maxSize;
        
        for (int i = 0; i < discardCount; i++) {
            entries.pollFirstEntry(); // remove largest
        }
        
        this.maxSize = maxSize;
    }
    
    public List<Node> dump() {
        return new ArrayList<>(entries.values());
    }

    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    public int size() {
        return entries.size();
    }

    public int getMaxSize() {
        return maxSize;
    }
    
    public enum TouchResult {
        INSERTED, // inserted as an entry
        REPLACED, // replaced an entry with this because id was closer than other ids (note that if maxSize == 0, you'll always get this)
        UPDATED, // entry already existed, so nothing was changed
        IGNORED, // entry with same id already existed, but link is different, so ignoring
    }

    @Override
    public String toString() {
        return "NearSet{" + "baseId=" + baseId + ", entries=" + entries + ", maxSize=" + maxSize + ", lastUpdateTime=" + lastUpdateTime
                + '}';
    }
    
}
