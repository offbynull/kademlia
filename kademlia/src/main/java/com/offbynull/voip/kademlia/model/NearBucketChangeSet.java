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
    private final NodeChangeSet bucketChangeSet;
    private final NodeChangeSet peerChangeSet;

    public NearBucketChangeSet(NodeChangeSet bucketChangeSet, NodeChangeSet peerChangeSet) {
        Validate.notNull(bucketChangeSet);
        Validate.notNull(peerChangeSet);
        this.bucketChangeSet = bucketChangeSet;
        this.peerChangeSet = peerChangeSet;
    }

    public NodeChangeSet getBucketChangeSet() {
        return bucketChangeSet;
    }

    public NodeChangeSet getPeerChangeSet() {
        return peerChangeSet;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.bucketChangeSet);
        hash = 83 * hash + Objects.hashCode(this.peerChangeSet);
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
        if (!Objects.equals(this.bucketChangeSet, other.bucketChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.peerChangeSet, other.peerChangeSet)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "NearBucketChangeSet{" + "nearChangeSet=" + bucketChangeSet + ", networkChangeSet=" + peerChangeSet + '}';
    }
}
