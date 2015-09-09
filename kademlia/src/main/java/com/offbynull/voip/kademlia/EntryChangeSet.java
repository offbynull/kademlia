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
package com.offbynull.voip.kademlia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import static java.util.Collections.emptyList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class EntryChangeSet {
    static final EntryChangeSet NO_CHANGE = new EntryChangeSet(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    
    private final UnmodifiableList<Entry> removed;
    private final UnmodifiableList<Entry> added;
    private final UnmodifiableList<UpdatedEntry> updated;
    
    public static EntryChangeSet added(Entry ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return added(Arrays.asList(entries));
    }

    public static EntryChangeSet added(Collection<Entry> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new EntryChangeSet(entries, emptyList(), emptyList());
    }

    public static EntryChangeSet removed(Entry ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return removed(Arrays.asList(entries));
    }

    public static EntryChangeSet removed(Collection<Entry> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new EntryChangeSet(emptyList(), entries, emptyList());
    }

    public static EntryChangeSet updated(UpdatedEntry ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return updated(Arrays.asList(entries));
    }

    public static EntryChangeSet updated(Collection<UpdatedEntry> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new EntryChangeSet(emptyList(), emptyList(), entries);
    }
    
    public EntryChangeSet(Collection<Entry> added, Collection<Entry> removed, Collection<UpdatedEntry> updated) {
        Validate.notNull(removed);
        Validate.notNull(added);
        Validate.notNull(updated);
        Validate.noNullElements(removed);
        Validate.noNullElements(added);
        Validate.noNullElements(updated);
        
        // ensure that there aren't any duplicate ids
        Set<Id> tempSet = new HashSet<>();
        removed.stream().map(x -> x.getNode().getId()).forEach(x -> tempSet.add(x));
        added.stream().map(x -> x.getNode().getId()).forEach(x -> tempSet.add(x));
        updated.stream().map(x -> x.getNode().getId()).forEach(x -> tempSet.add(x));
        Validate.isTrue(tempSet.size() == added.size() + removed.size() + updated.size());

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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.removed);
        hash = 47 * hash + Objects.hashCode(this.added);
        hash = 47 * hash + Objects.hashCode(this.updated);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntryChangeSet other = (EntryChangeSet) obj;
        if (!Objects.equals(this.removed, other.removed)) {
            return false;
        }
        if (!Objects.equals(this.added, other.added)) {
            return false;
        }
        if (!Objects.equals(this.updated, other.updated)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ChangeSet{" + "removed=" + removed + ", added=" + added + ", updated=" + updated + '}';
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

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 79 * hash + Objects.hashCode(this.node);
            hash = 79 * hash + Objects.hashCode(this.oldLastSeenTime);
            hash = 79 * hash + Objects.hashCode(this.newLastSeenTime);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final UpdatedEntry other = (UpdatedEntry) obj;
            if (!Objects.equals(this.node, other.node)) {
                return false;
            }
            if (!Objects.equals(this.oldLastSeenTime, other.oldLastSeenTime)) {
                return false;
            }
            if (!Objects.equals(this.newLastSeenTime, other.newLastSeenTime)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "UpdatedEntry{" + "node=" + node + ", oldLastSeenTime=" + oldLastSeenTime + ", newLastSeenTime=" + newLastSeenTime + '}';
        }
        
    }
}
