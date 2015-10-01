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

import org.apache.commons.lang3.Validate;

public final class IdPrefixMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Id id;
    private final BitString expectedPrefix;

    public IdPrefixMismatchException(Id id, BitString expectedPrefix) {
        super("ID prefix mismatch (required " + expectedPrefix +  "): " + id + ")");
        Validate.notNull(id);
        Validate.notNull(expectedPrefix);
        
        // what's the point of throwing an exception for not having a shared prefix if you have a shared prefix?
        Validate.isTrue(id.getBitString().getSharedPrefixLength(expectedPrefix) != expectedPrefix.getBitLength());
        this.id = id;
        this.expectedPrefix = expectedPrefix;
    }

    public Id getId() {
        return id;
    }

    public BitString getExpectedPrefix() {
        return expectedPrefix;
    }


}
