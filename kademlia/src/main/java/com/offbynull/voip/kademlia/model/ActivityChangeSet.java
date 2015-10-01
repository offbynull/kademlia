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
package com.offbynull.voip.kademlia.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import static java.util.Collections.emptyList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class ActivityChangeSet {
    static final ActivityChangeSet NO_CHANGE = new ActivityChangeSet(emptyList(), emptyList(), emptyList());
    
    private final UnmodifiableList<Activity> removed;
    private final UnmodifiableList<Activity> added;
    private final UnmodifiableList<Activity> updated;
    
    public static ActivityChangeSet added(Activity ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return added(Arrays.asList(entries));
    }

    public static ActivityChangeSet added(Collection<Activity> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new ActivityChangeSet(entries, emptyList(), emptyList());
    }

    public static ActivityChangeSet removed(Activity ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return removed(Arrays.asList(entries));
    }

    public static ActivityChangeSet removed(Collection<Activity> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new ActivityChangeSet(emptyList(), entries, emptyList());
    }

    public static ActivityChangeSet updated(Activity ... entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return updated(Arrays.asList(entries));
    }

    public static ActivityChangeSet updated(Collection<Activity> entries) {
        Validate.notNull(entries);
        Validate.noNullElements(entries);
        return new ActivityChangeSet(emptyList(), emptyList(), entries);
    }
    
    public ActivityChangeSet(Collection<Activity> added, Collection<Activity> removed, Collection<Activity> updated) {
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

        this.removed = (UnmodifiableList<Activity>) UnmodifiableList.unmodifiableList(new ArrayList<>(removed));
        this.added = (UnmodifiableList<Activity>) UnmodifiableList.unmodifiableList(new ArrayList<>(added));
        this.updated = (UnmodifiableList<Activity>) UnmodifiableList.unmodifiableList(new ArrayList<>(updated));
    }

    public UnmodifiableList<Activity> viewRemoved() {
        return removed;
    }

    public UnmodifiableList<Activity> viewAdded() {
        return added;
    }

    public UnmodifiableList<Activity> viewUpdated() {
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
        final ActivityChangeSet other = (ActivityChangeSet) obj;
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
        return "ActivityChangeSet{" + "removed=" + removed + ", added=" + added + ", updated=" + updated + '}';
    }
}
