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

import java.util.Comparator;
import org.apache.commons.lang3.Validate;

public final class IdClosenessComparator implements Comparator<Id> {
    private final Id baseId;

    public IdClosenessComparator(Id baseId) {
        Validate.notNull(baseId);
        this.baseId = baseId;
    }

    @Override
    public int compare(Id o1, Id o2) {
        Validate.notNull(o1);
        Validate.notNull(o2);
        Validate.isTrue(!baseId.equals(o1)); // o1 can't equal the base ID
        Validate.isTrue(!baseId.equals(o2)); // o2 can't equal the base ID
        
        // If the two IDs contain all the same bits, then they're equal
        if (o1.equals(o2)) {
            return 0;
        }
        
        // If one ID contains a larger common prefix with base ID than the other ID, that one with the larger prefix is the greater one
        int simpleCompareRes = compareBySharedPrefixLength(o1, o2);
        if (simpleCompareRes != 0) {
            return simpleCompareRes;
        }
        
        // If the prefixes are equal, start flipping bits after sharedPrefixLen until one comes out greater than the other...
        // See section in notes about notion of closeness to understand why this is being done
        int offset = o1.getSharedPrefixLength(baseId);
        int end = baseId.getBitLength();
        for (int i = offset; i < end; i++) {
            o1 = o1.flipBit(i);
            o2 = o2.flipBit(i);
            
            int prefixLenCompareRes = compareBySharedPrefixLength(o1, o2);
            if (prefixLenCompareRes != 0) {
                return prefixLenCompareRes;
            }
        }
        
        // You should never make it to this point, since this means that o1 and o2 are equal. We have earlier checks that should return
        // if o1 and o2 are equal
        throw new IllegalStateException();
    }

    private int compareBySharedPrefixLength(Id o1, Id o2) {
        int sharedPrefixLen1 = o1.getSharedPrefixLength(baseId);
        int sharedPrefixLen2 = o2.getSharedPrefixLength(baseId);
        
        if (sharedPrefixLen1 > sharedPrefixLen2) {
            return 1;
        } else if (sharedPrefixLen1 < sharedPrefixLen2) {
            return -1;
        } else {
            return 0;
        }
    }
    
}
