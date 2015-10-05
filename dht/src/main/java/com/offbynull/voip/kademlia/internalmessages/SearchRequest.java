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

/**
 * A request to traverse the Kademlia network to find the closest nodes to some ID.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class SearchRequest {
    private final Id findId;
    private final int maxResults;

    /**
     * Constructs a {@link SearchRequest} object.
     * @param findId ID to find
     * @param maxResults maximum number of results to return
     * @throws NullPointerException if {@code findId} is {@code null}
     * @throws IllegalArgumentException if any numeric argument is negative
     */
    public SearchRequest(Id findId, int maxResults) {
        Validate.notNull(findId);
        Validate.isTrue(maxResults >= 0); // why would anyone want 0? let thru anyway
        this.findId = findId;
        this.maxResults = maxResults;
    }
    
    /**
     * Get the ID to find.
     * @return ID to find
     */
    public Id getFindId() {
        return findId;
    }

    /**
     * Get the maximum number of results to return.
     * @return maximum number of results to return
     */
    public int getMaxResults() {
        return maxResults;
    }
    
}
