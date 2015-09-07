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
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.lang3.Validate;

public final class MostRecentlySeenSet {
    private final Id baseId;
    private final LinkedList<Entry> entries;

    private int maxSize;

    public MostRecentlySeenSet(Id baseId, int maxSize) {
        Validate.notNull(baseId);
        Validate.isTrue(maxSize >= 0);
        
        this.baseId = baseId;
        this.maxSize = maxSize;

        this.entries = new LinkedList<>();
    }
  
    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(!nodeId.equals(baseId));
        
        // TODO: You can make this way more efficient if you used something like MultiTreeSet (guava) and sorted based on entry time

        // Remove existing entry
        ListIterator<Entry> it = entries.listIterator();
        while (it.hasNext()) {
            Entry entry = it.next();

            Id entryId = entry.getNode().getId();
            String entryLink = entry.getNode().getLink();

            if (entryId.equals(nodeId)) {
                if (!entryLink.equals(nodeLink)) {
                    // if ID exists but link for ID is different
                    return TouchResult.CONFLICTED;
                }

                // remove and add to tail (most recently-seen)
                it.remove();
                break;
            }
        }

        
        // Add entry
        Entry newEntry = new Entry(node, time);
        
        it = entries.listIterator(entries.size());
        boolean added = false;
        while (it.hasPrevious()) {
            Entry entry = it.previous();

            if (entry.getLastSeenTime().isBefore(time)) {
                it.next(); // move forward 1 space, we want to add to element just after entry
                it.add(newEntry);
                added = true;
                break;
            }
        }

        if (!added) { // special case where newEntry needs to be added at the end of entries, not handled by loop above
            entries.addFirst(newEntry);
        }

        
        // Set has become too large, remove the item with the earliest time
        if (entries.size() > maxSize) {
            // if the node removed with the earliest time is the one we just added, then report that node couldn't be added
            Entry discardedEntry = entries.removeFirst();
            if (discardedEntry == newEntry) {
                return TouchResult.IGNORED;
            }
        }

        
        // Add successful
        return TouchResult.UPDATED;
    }

    public RemoveResult remove(Node node) {
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        ListIterator<Entry> it = entries.listIterator();
        while (it.hasNext()) {
            Entry entry = it.next();

            Id entryId = entry.getNode().getId();
            String entryLink = entry.getNode().getLink();

            if (entryId.equals(nodeId)) {
                if (!entryLink.equals(nodeLink)) {
                    // if ID exists but link for ID is different
                    return RemoveResult.CONFLICTED;
                }

                // remove
                it.remove();
                return RemoveResult.REMOVED;
            }
        }
        
        return RemoveResult.NOT_FOUND;
    }

    public void resize(int maxSize) {
        Validate.isTrue(maxSize >= 1);
        
        int discardCount = this.maxSize - maxSize;
        
        for (int i = 0; i < discardCount; i++) {
            entries.removeFirst(); // remove node that hasn't been touched the longest
        }
        
        this.maxSize = maxSize;
    }

    public Entry take() {
        Entry e = entries.removeLast();
        if (e == null) {
            return null;
        }
        return e;
    }
    
    public List<Entry> dump() {
        return new ArrayList<>(entries);
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
        return "MostRecentlySeenSet{" + "baseId=" + baseId + ", entries=" + entries + ", maxSize=" + maxSize + '}';
    }



}
