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
import java.util.Collection;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import org.apache.commons.collections4.list.UnmodifiableList;
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
            return ChangeSet.EMPTY;
        }
        
        List<Entry> added = new ArrayList<>(1);
        List<Entry> removed = new ArrayList<>(1);
        List<UpdatedEntry> updated = new ArrayList<>(1);
        
        Entry newEntry = new Entry(node, time);
        Entry existingEntry;
        if ((existingEntry = entries.get(nodeId)) != null) {
            if (!existingEntry.getNode().equals(node)) {
                // if ID exists but link for ID is different, ignore
                return ChangeSet.EMPTY;
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
            return ChangeSet.EMPTY;
        }

        Id entryId = entry.getNode().getId();
        String entryLink = entry.getNode().getLink();

        Validate.validState(nodeId.equals(entryId)); // should never happen -- just in case
        if (!entryLink.equals(nodeLink)) {
            // if ID exists but link for ID is different
            return ChangeSet.EMPTY;
        }

        // remove
        entries.remove(nodeId);
        return new ChangeSet(emptyList(), singletonList(entry), emptyList());
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
        
        return new ChangeSet(emptyList(), removed, emptyList());
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

    public static final class ChangeSet { 
        private static final ChangeSet EMPTY = new ChangeSet(emptyList(), emptyList(), emptyList());
        private final UnmodifiableList<Entry> removed;
        private final UnmodifiableList<Entry> added;
        private final UnmodifiableList<UpdatedEntry> updated;
        
        public ChangeSet(Collection<Entry> added, Collection<Entry> removed, Collection<UpdatedEntry> updated) {
            Validate.notNull(removed);
            Validate.notNull(added);
            Validate.notNull(updated);
            Validate.noNullElements(removed);
            Validate.noNullElements(added);
            Validate.noNullElements(updated);
            this.removed = (UnmodifiableList<Entry>) UnmodifiableList.unmodifiableList(new ArrayList<>(removed));
            this.added = (UnmodifiableList<Entry>) UnmodifiableList.unmodifiableList(new ArrayList<>(added));
            this.updated = (UnmodifiableList<UpdatedEntry>) UnmodifiableList.unmodifiableList(new ArrayList<>(updated));
        }

        public UnmodifiableList<Entry> viewRemoved() {
            return removed;
        }

        public UnmodifiableList<Entry> viewAdded() {
            return added;
        }

        public UnmodifiableList<UpdatedEntry> viewUpdated() {
            return updated;
        }
    }
    
    public static final class UpdatedEntry {
        private final Node node;
        private final Instant oldLastSeenTime;
        private final Instant newLastSeenTime;

        public UpdatedEntry(Node node, Instant oldLastSeenTime, Instant newLastSeenTime) {
            Validate.notNull(node);
            Validate.notNull(oldLastSeenTime);
            Validate.notNull(newLastSeenTime);
            
            this.node = node;
            this.oldLastSeenTime = oldLastSeenTime;
            this.newLastSeenTime = newLastSeenTime;
        }

        public Node getNode() {
            return node;
        }

        public Instant getOldLastSeenTime() {
            return oldLastSeenTime;
        }

        public Instant getNewLastSeenTime() {
            return newLastSeenTime;
        }
        
    }
    
    @Override
    public String toString() {
        return "NearSet{" + "baseId=" + baseId + ", entries=" + entries + ", maxSize=" + maxSize + '}';
    }
    
}
