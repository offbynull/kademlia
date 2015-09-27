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

import java.io.Serializable;
import java.util.Comparator;
import org.apache.commons.lang3.Validate;

public final class IdEuclideanMetricComparator implements Comparator<Id>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id baseId;

    public IdEuclideanMetricComparator(Id baseId) {
        Validate.notNull(baseId);
        this.baseId = baseId;
    }

    // Compares as if numbers in a straight line / numberline...
    //
    // So for example, nodes would be ordered like...
    //
    // 0000 (0)
    // 0001 (1)
    // 0010 (2)
    // 0011 (2)
    // ...
    @Override
    public int compare(Id o1, Id o2) {
        Validate.notNull(o1);
        Validate.notNull(o2);
        InternalValidate.matchesLength(baseId.getBitLength(), o1);
        InternalValidate.matchesLength(baseId.getBitLength(), o2);
        
        int bitLen = baseId.getBitLength();
        
        int offset = 0;
        while (offset < bitLen) {
            // read as much as possible, up to 63 bits
            // 63 because the 64th bit will be the sign bit, and we don't want to deal negatives
            int readLen = Math.min(bitLen - offset, 63);
            
            // grab next group of bits from o1 and o2
            long block1 = o1.getBitsAsLong(offset, readLen);
            long block2 = o2.getBitsAsLong(offset, readLen);
            
            // move offset by the amount we read
            offset += readLen;
            
            // if we read hte full 64 bits, we'd have to use compareUnsigned? we don't want to do that because behind the scenes if creates
            // a BigInteger and does the comparison using that.
            //
            // compare 63 bits together, if not equal, we've found a "greater" one (remember how < operation works first unequal bit is the
            // one used to determine which is greater and which is less)
            int res = Long.compare(block1, block2);
            if (res != 0) {
                return res;
            }
        }
        
        // Reaching this point means that o1 and o2 match baseId.
        return 0;
    }
    
}
