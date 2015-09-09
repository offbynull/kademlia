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

public final class NodeChangeSet {
    static final NodeChangeSet NO_CHANGE = new NodeChangeSet(Collections.emptyList(), Collections.emptyList());
    
    private final UnmodifiableList<Node> removed;
    private final UnmodifiableList<Node> added;
    
    public static NodeChangeSet added(Node ... nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        return added(Arrays.asList(nodes));
    }

    public static NodeChangeSet added(Collection<Node> nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        return new NodeChangeSet(nodes, emptyList());
    }

    public static NodeChangeSet removed(Node ... nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        return removed(Arrays.asList(nodes));
    }

    public static NodeChangeSet removed(Collection<Node> nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        return new NodeChangeSet(emptyList(), nodes);
    }
    
    public NodeChangeSet(Collection<Node> added, Collection<Node> removed) {
        Validate.notNull(removed);
        Validate.notNull(added);
        Validate.noNullElements(removed);
        Validate.noNullElements(added);
        
        // ensure that there aren't any duplicate ids
        Set<Id> tempSet = new HashSet<>();
        removed.stream().map(x -> x.getId()).forEach(x -> tempSet.add(x));
        added.stream().map(x -> x.getId()).forEach(x -> tempSet.add(x));
        Validate.isTrue(tempSet.size() == added.size() + removed.size());
        
        this.removed = (UnmodifiableList<Node>) UnmodifiableList.unmodifiableList(new ArrayList<>(removed));
        this.added = (UnmodifiableList<Node>) UnmodifiableList.unmodifiableList(new ArrayList<>(added));
    }

    public UnmodifiableList<Node> viewRemoved() {
        return removed;
    }

    public UnmodifiableList<Node> viewAdded() {
        return added;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + Objects.hashCode(this.removed);
        hash = 73 * hash + Objects.hashCode(this.added);
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
        final NodeChangeSet other = (NodeChangeSet) obj;
        if (!Objects.equals(this.removed, other.removed)) {
            return false;
        }
        if (!Objects.equals(this.added, other.added)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NodeChangeSet{" + "removed=" + removed + ", added=" + added + '}';
    }

}
