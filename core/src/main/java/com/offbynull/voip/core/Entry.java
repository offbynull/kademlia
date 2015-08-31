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
import org.apache.commons.lang3.Validate;


final class Entry {
    private final Node node;
    private final Instant insertTime;
    private Instant lastSeenTime;

    public Entry(Node node, Instant insertTime) {
        Validate.notNull(node);
        Validate.notNull(insertTime);
        this.node = node;
        this.lastSeenTime = insertTime;
        this.insertTime = insertTime;
    }

    public Node getNode() {
        return node;
    }

    public Instant getLastSeenTime() {
        return lastSeenTime;
    }

    public Instant getInsertTime() {
        return insertTime;
    }

    public void setLastSeenTime(Instant lastSeenTime) {
        Validate.notNull(lastSeenTime);
        Validate.isTrue(!this.lastSeenTime.isAfter(lastSeenTime));
        this.lastSeenTime = lastSeenTime;
    }
}
