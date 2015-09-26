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

public final class RouterChangeSet {
    private final RouteTreeChangeSet routeTreeChangeSet;
    private final NearBucketChangeSet nearBucketChangeSet;

    public RouterChangeSet(RouteTreeChangeSet routeTreeChangeSet, NearBucketChangeSet nearBucketChangeSet) {
        Validate.notNull(routeTreeChangeSet);
        Validate.notNull(nearBucketChangeSet);
        this.routeTreeChangeSet = routeTreeChangeSet;
        this.nearBucketChangeSet = nearBucketChangeSet;
    }

    public RouteTreeChangeSet getRouteTreeChangeSet() {
        return routeTreeChangeSet;
    }

    public NearBucketChangeSet getNearBucketChangeSet() {
        return nearBucketChangeSet;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.routeTreeChangeSet);
        hash = 79 * hash + Objects.hashCode(this.nearBucketChangeSet);
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
        final RouterChangeSet other = (RouterChangeSet) obj;
        if (!Objects.equals(this.routeTreeChangeSet, other.routeTreeChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.nearBucketChangeSet, other.nearBucketChangeSet)) {
            return false;
        }
        return true;
    }

}
