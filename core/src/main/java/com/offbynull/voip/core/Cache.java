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
 * Most-recently seen cache for Kademlia k-buckets. A cache will hold up to a certain number of the most recently seen nodes.
 * <ul>
 * <li>Adding a node to a cache that isn't full will add that node as the latest node in the cache.</li>
 * <li>Adding a node to a cache that is full will add that node as the latest node in the cache, as well as remove the earliest node in the
 * cache.</li>
 * <li>Adding a node that already exists in the cache will move that node to the latest node in the cache.</li>
 * </ul>
 * @author Kasra Faghihi
 */
public final class Cache {
    private final BitString prefix;
    private final int idBitLength;

    private final LinkedList<Entry> entries;
    
    private int maxSize;
    private Instant lastUpdateTime;

    public Cache(BitString prefix, int idBitLength, int maxSize) {
        Validate.notNull(prefix);
        Validate.isTrue(idBitLength >= prefix.getBitLength());
        Validate.isTrue(maxSize > 0);
        
        this.prefix = prefix;
        this.idBitLength = idBitLength;
        this.maxSize = maxSize;

        this.entries = new LinkedList<>();
        
        lastUpdateTime = Instant.MIN;
    }
    
    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Validate.isTrue(nodeId.getBitLength() == idBitLength);
        Validate.isTrue(nodeId.getBitString().getSharedPrefixLength(prefix) == prefix.getBitLength());
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        // If not true, this essentially means that this.id and id are equal, so what's the point of having a bucket? Validate here?
        // Validate.isTrue(commonPrefixBitCount < id.getBitLength());
        
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

        // Add
        Entry entry = new Entry(node, time);
        entries.addLast(entry);
        
        if (entries.size() > maxSize) {
            entries.removeFirst();
            return TouchResult.REPLACED;
        }
        
        return TouchResult.INSERTED;
    }

    public Node take() {
        Entry e = entries.removeLast();
        if (e == null) {
            return null;
        }
        return e.getNode();
    }
    
    // bitCount = 1 is 2 caches
    // bitCount = 2 is 4 caches
    // bitCount = 3 is 8 caches
    public Cache[] split(int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to split in to 2^30 cachess, but whatever
                                         // we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new Cache[1 << 31] -- 1 << 31 is negative
                                         // new Cache[1 << 30] -- 1 << 30 is positive
        
        Validate.isTrue(prefix.getBitLength() + bitCount <= idBitLength);

        // Create caches 
        BitString[] newPrefixes = InternalUtils.appendToBitString(prefix, bitCount);
        Cache[] newCaches = new Cache[newPrefixes.length];
        for (int i = 0; i < newCaches.length; i++) {
            newCaches[i] = new Cache(newPrefixes[i], idBitLength, maxSize);
        }
        
        // Place entries in cache
        for (Entry entry : entries) {
            Node node = entry.getNode();
            
            // Read bitCount bits starting from prefixBitSize and use that to figure out which cache to copy to
            // For example, if bitCount is 2 ...
            // If you read 00b, 00 = 0, so this ID will be go to newCache[0]
            // If you read 01b, 01 = 1, so this ID will be go to newCache[1]
            // If you read 10b, 10 = 2, so this ID will be go to newCache[2]
            // If you read 11b, 11 = 3, so this ID will be go to newCache[3]
            Id id = node.getId();
            int idx = (int) id.getBitsAsLong(prefix.getBitLength(), bitCount);
            
            TouchResult res;
            res = newCaches[idx].touch(entry.getInsertTime(), node); // first call to touch should add with insert time
            Validate.validState(res == TouchResult.INSERTED); // should always happen, but just in case
            res = newCaches[idx].touch(entry.getLastSeenTime(), node); // send call to touch should set laset seen time
            Validate.validState(res == TouchResult.UPDATED); // should always happen, but just in case
        }
        
        return newCaches;
    }
    
    public void resize(int maxSize) {
        Validate.isTrue(maxSize >= 1);
        
        int discardCount = this.maxSize - maxSize;
        
        for (int i = 0; i < discardCount; i++) {
            entries.removeFirst(); // remove oldest
        }
        
        this.maxSize = maxSize;
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
        INSERTED, // inserted as latest entry
        REPLACED, // cache is full so removed earliest entry and inserted this entry as latest
        UPDATED, // entry moved to latest entry
        IGNORED, // entry with same id already existed, but link is different, so ignoring
    }
}
