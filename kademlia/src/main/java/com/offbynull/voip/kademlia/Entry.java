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
import java.util.Objects;
import org.apache.commons.lang3.Validate;


public final class Entry {
    private final Node node;
    private final Instant lastSeenTime;

    public Entry(Node node, Instant lastSeenTime) {
        Validate.notNull(node);
        Validate.notNull(lastSeenTime);
        this.node = node;
        this.lastSeenTime = lastSeenTime;
    }

    public Node getNode() {
        return node;
    }

    public Instant getLastSeenTime() {
        return lastSeenTime;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.node);
        hash = 31 * hash + Objects.hashCode(this.lastSeenTime);
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
        final Entry other = (Entry) obj;
        if (!Objects.equals(this.node, other.node)) {
            return false;
        }
        if (!Objects.equals(this.lastSeenTime, other.lastSeenTime)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Entry{" + "node=" + node + ", lastSeenTime=" + lastSeenTime + '}';
    }
    
}
