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

import java.io.Serializable;
import java.util.Comparator;
import org.apache.commons.lang3.Validate;

public final class IdClosenessComparator implements Comparator<Id>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id baseId;

    public IdClosenessComparator(Id baseId) {
        Validate.notNull(baseId);
        this.baseId = baseId;
    }


    // This is the XOR metric
    @Override
    public int compare(Id o1, Id o2) {
        Validate.notNull(o1);
        Validate.notNull(o2);
        Validate.isTrue(o1.getBitLength() == baseId.getBitLength(), "Bitlengths between IDs must be equal to that of base ID");
        Validate.isTrue(o2.getBitLength() == baseId.getBitLength(), "Bitlengths between IDs must be equal to that of base ID");
        
        int bitLen = baseId.getBitLength();
        
        int offset = 0;
        while (offset < bitLen) {
            // read as much as possible, up to 63 bits
            // 63 because the 64th bit will be the sign bit, and we don't want to deal negatives
            int readLen = Math.min(bitLen - offset, 63);
            
            // xor blocks together
            long xorBlock1 = o1.getBitsAsLong(offset, readLen) ^ baseId.getBitsAsLong(offset, readLen);
            long xorBlock2 = o2.getBitsAsLong(offset, readLen) ^ baseId.getBitsAsLong(offset, readLen);
            
            // move offset by the amount we read
            offset += readLen;
            
            // if we read hte full 64 bits, we'd have to use compareUnsigned? we don't want to do that because behind the scenes if creates
            // a BigInteger and does the comparison using that.
            //
            // compare 63 bits together, if not equal, we've found a "greater" one
            int res = Long.compare(xorBlock1, xorBlock2);
            if (res != 0) {
                return res;
            }
        }
        
        // Reaching this point means that o1 and o2 match baseId.
        return 0;
    }
    
}
