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

import java.util.TreeMap;
import org.apache.commons.lang3.Validate;

public final class CloseSet {
    private final Id baseId;
    private final TreeMap<Id, Node> entries;
    
    private final int maxSize;

    public CloseSet(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 1);
        
        this.baseId = baseId;
        this.entries = new TreeMap<>(new IdClosenessComparator(baseId));
        this.maxSize = maxSize;
    }

    public TouchResult touch(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        Node existingNode;
        if ((existingNode = entries.get(nodeId)) != null) {
            if (!existingNode.equals(node)) {
                // if ID exists but link for ID is different, ignore
                return TouchResult.IGNORED;
            }
            
            return TouchResult.UPDATED;
        }
        
        entries.put(nodeId, node);
        if (entries.size() <= maxSize) {
            return TouchResult.INSERTED;
        }
        
        entries.pollFirstEntry(); // remove first entry so we don't exceed maxSize
        return TouchResult.REPLACED;
    }
    
    public enum TouchResult {
        INSERTED, // inserted as an entry
        REPLACED, // replaced an entry with this because id was closer than other ids
        UPDATED, // entry already existed, so nothing was changed
        IGNORED, // entry with same id already existed, but link is different, so ignoring
    }
}
