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

import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class NearBucketChangeSet {
    private final NodeChangeSet beforeBucketChangeSet;
    private final NodeChangeSet afterBucketChangeSet;
    private final NodeChangeSet cacheChangeSet;

    public NearBucketChangeSet(NodeChangeSet beforeBucketChangeSet, NodeChangeSet afterBucketChangeSet, NodeChangeSet cacheChangeSet) {
        Validate.notNull(beforeBucketChangeSet);
        Validate.notNull(afterBucketChangeSet);
        Validate.notNull(cacheChangeSet);
        this.beforeBucketChangeSet = beforeBucketChangeSet;
        this.afterBucketChangeSet = afterBucketChangeSet;
        this.cacheChangeSet = cacheChangeSet;
    }

    public NodeChangeSet getBeforeBucketChangeSet() {
        return beforeBucketChangeSet;
    }

    public NodeChangeSet getAfterBucketChangeSet() {
        return afterBucketChangeSet;
    }

    public NodeChangeSet getCacheChangeSet() {
        return cacheChangeSet;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.beforeBucketChangeSet);
        hash = 13 * hash + Objects.hashCode(this.afterBucketChangeSet);
        hash = 13 * hash + Objects.hashCode(this.cacheChangeSet);
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
        final NearBucketChangeSet other = (NearBucketChangeSet) obj;
        if (!Objects.equals(this.beforeBucketChangeSet, other.beforeBucketChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.afterBucketChangeSet, other.afterBucketChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.cacheChangeSet, other.cacheChangeSet)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NearBucketChangeSet{" + "beforeBucketChangeSet=" + beforeBucketChangeSet + ", afterBucketChangeSet="
                + afterBucketChangeSet + ", cacheChangeSet=" + cacheChangeSet + '}';
    }

}
