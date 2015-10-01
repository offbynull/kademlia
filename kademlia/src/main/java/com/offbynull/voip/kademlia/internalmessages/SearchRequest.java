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
package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.voip.kademlia.model.Id;
import org.apache.commons.lang3.Validate;

public final class SearchRequest {
    private final Id findId;
    private final int maxResults;

    public SearchRequest(Id findId, int maxResults) {
        Validate.notNull(findId);
        Validate.isTrue(maxResults >= 0); // why would anyone want 0? let thru anyway
        this.findId = findId;
        this.maxResults = maxResults;
    }
    
    public Id getFindId() {
        return findId;
    }

    public int getMaxResults() {
        return maxResults;
    }
    
}
