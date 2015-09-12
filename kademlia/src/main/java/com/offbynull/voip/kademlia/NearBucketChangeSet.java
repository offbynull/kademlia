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

import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class NearBucketChangeSet {
    private final NodeChangeSet nearChangeSet;
    private final NodeChangeSet networkChangeSet;

    public NearBucketChangeSet(NodeChangeSet nearChangeSet, NodeChangeSet networkChangeSet) {
        Validate.notNull(nearChangeSet);
        Validate.notNull(networkChangeSet);
        this.nearChangeSet = nearChangeSet;
        this.networkChangeSet = networkChangeSet;
    }

    public NodeChangeSet getBucketChangeSet() {
        return nearChangeSet;
    }

    public NodeChangeSet getCacheChangeSet() {
        return networkChangeSet;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.nearChangeSet);
        hash = 83 * hash + Objects.hashCode(this.networkChangeSet);
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
        if (!Objects.equals(this.nearChangeSet, other.nearChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.networkChangeSet, other.networkChangeSet)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NearBucketChangeSet{" + "nearChangeSet=" + nearChangeSet + ", networkChangeSet=" + networkChangeSet + '}';
    }
}
