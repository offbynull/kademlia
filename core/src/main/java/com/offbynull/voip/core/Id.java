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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * Kademlia ID. Size of ID is configurable (in bits).
 * <p>
 * Class is immutable.
 * @author Kasra Faghihi
 */
public final class Id implements Serializable {
    private static final long serialVersionUID = 1L;

    private final BitString bitString;
    
    // make sure that whatever you pass in as data is a copy / not-shared.
    private Id(BitString bitString) {
        Validate.notNull(bitString);
        this.bitString = bitString;
    }

    /**
     * Constructs an {@link Id} from a byte array.
     * @param data id value
     * @param bitLength number of bits in this id
     * @return created id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code bitLength <= 0}, or if {@code data} is larger than the minimum number of bytes that it
     * takes to retain {@code bitLength} (e.g. if you're retaining 12 bits, you need 2 bytes or less -- {@code 12/8 + (12%8 == 0 ? 0 : 1)})
     */
    public static Id create(byte[] data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);
        
        return new Id(BitString.create(data, bitLength));
    }
    
    /**
     * Constructs an {@link Id} from a long.
     * @param data id value
     * @param bitLength number of total bits allowed in the id (must be between 0 and 63 inclusive)
     * @return created id
     * @throws IllegalArgumentException if {@code 64 < bitLength < 1}
     */
    public static Id createFromLong(long data, int bitLength) {
        Validate.isTrue(bitLength > 0);
        Validate.isTrue(bitLength < 64);
        
        return new Id(BitString.createFromLong(data, bitLength));
    }

    /**
     * Get the number of bits that are the same between this ID and another ID
     * @param other other ID to test against
     * @return number of common prefix bits
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the bitlength from {@code this} doesn't match the bitlength from {@code other}
     */
    public int getSharedPrefixLength(Id other) {
        Validate.notNull(other);
        Validate.isTrue(bitString.getBitLength() == other.bitString.getBitLength());

        return bitString.getSharedPrefixLength(other.bitString);
    }
    
    /**
     * Get bit from this ID. Bit 0 is the left-most bit, bit 1 the second left-most bit, etc. For example, ID 0x3C...
     * <pre>
     * 3    C
     * 0011 1100
     * |  | |  |
     * 0  3 4  7
     * </pre>
     * @param index index of bit
     * @return {@code true} if bit is 1, {@code false} if bit is 0
     * @throws IllegalArgumentException if {@code index < 0} or if {@code index > bitLength}
     */
    public boolean getBit(int index) {
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < bitString.getBitLength());
        
        return bitString.getBit(index);
    }

    /**
     * Set bit within a copy of this ID. Bit 0 is the left-most bit, bit 1 the second left-most bit, etc. For example, ID 0x3C...
     * <pre>
     * 3    C
     * 0011 1100
     * |  | |  |
     * 0  3 4  7
     * </pre>
     * @param index index of bit
     * @param bit {@code true} if bit is 1, {@code false} if bit is 0
     * @return new ID that has bit set
     * @throws IllegalArgumentException if {@code index < 0} or if {@code index > bitLength}
     */
    public Id setBit(int index, boolean bit) {
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < bitString.getBitLength());
        
        return new Id(bitString.setBit(index, bit));
    }

    /**
     * Flip bit within a copy of this ID.
     * @param index index of bit
     * @return new ID that has bit flipped
     * @throws IllegalArgumentException if {@code index < 0} or if {@code index > bitLength}
     */
    public Id flipBit(int index) {
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < bitString.getBitLength());
        
        boolean bit = getBit(index);
        return setBit(index, !bit);
    }

    /**
     * Gets the maximum bit length for this ID.
     * @return max bit length for ID
     */
    public int getBitLength() {
        return bitString.getBitLength();
    }

    /**
     * Gets a copy of the data for this ID as a bitstring. You can convert this to a BigInteger via
     * {@code new BigInteger(1, getBitString().getData())}.
     * @return ID as bit string
     */
    public BitString getBitString() {
        return bitString;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 31 * hash + Objects.hashCode(this.bitString);
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
        final Id other = (Id) obj;
        if (!Objects.equals(this.bitString, other.bitString)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Id{" + "bitString=" + bitString + '}';
    }
}