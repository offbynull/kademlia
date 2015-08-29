/*
 * Copyright (c) 2013-2015, Kasra Faghihi, All rights reserved.
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
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

/**
 * A Kademlia ID. Bit-size of ID is configurable.
 * @author Kasra Faghihi
 */
public final class Id implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] data;
    private final int bitLength;
    
    // make sure that whatever you pass in as data is a copy / not-shared.
    private Id(byte[] data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);
        
        int minLength = calculateRequiredByteArraySize(bitLength);
        Validate.isTrue(data.length == minLength);
        
        this.data = data;
        this.bitLength = bitLength;
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
        
        int length = calculateRequiredByteArraySize(bitLength);
        Validate.isTrue(data.length <= length);
        
        // Create copy. Copy is of size length. If data is less than length, extra 0's are added as prefix.
        byte[] dataCopy = new byte[length];
        System.arraycopy(data, 0, dataCopy, length - data.length, data.length);
        
        clearUnusedTopBits(bitLength, dataCopy);
        return new Id(dataCopy, bitLength);
    }
    
    /**
     * Constructs a {@link Id} where a contiguous number of bits, starting from position 0, are set to 1. For example, if you want an id
     * that is {@code 00001111 11111111}, you would set {@code count} to 12, and {@code bitLength} to 16.
     * @param count number of 1 bits in id
     * @param bitLength number of total bits allowed in the id
     * @return created id
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code count < 0}, or if {@code bitLength <= 0}, or if {@code bitLength < count}
     */
    public static Id createContiguous(int count, int bitLength) {
        Validate.isTrue(count >= 0);
        Validate.isTrue(bitLength > 0);
        Validate.isTrue(bitLength >= count);
        
        int byteLength = calculateRequiredByteArraySize(bitLength);
        byte[] data = new byte[byteLength];
        
        insertOneBits(count, data);
        
        clearUnusedTopBits(bitLength, data); // don't need to do this here because insertOneBits already makes sure unused top bits are 0'd,
                                             // but do it just to be safe
        return new Id(data, bitLength);
    }
    
    /**
     * Constructs an {@link Id} from a long.
     * @param data id value
     * @param bitLength number of total bits allowed in the id (must be between 0 and 63 inclusive)
     * @return created id
     * @throws IllegalArgumentException if {@code 64 < bitLength < 1}
     */
    public static Id createFromInteger(long data, int bitLength) {
        Validate.isTrue(bitLength > 0);
        Validate.isTrue(bitLength < 64);
        
        int length = calculateRequiredByteArraySize(bitLength);
        
        byte[] bytes = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            int insertIntoIdx = length - i - 1;
            int shiftLeftAmount = i * 8;
            bytes[insertIntoIdx] = (byte) (data >>> shiftLeftAmount);
        }
        
        clearUnusedTopBits(bitLength, bytes);
        return new Id(bytes, bitLength);
    }
    
    /**
     * Populates a byte array with {@code bitLength} contiguous 1 bits, starting from bit position 0.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code bitLength = 1} results in 1 byte -- 00000001b<li/>
     * <li>{@code bitLength = 2} results in 1 byte -- 00000011b<li/>
     * <li>{@code bitLength = 8} results in 1 byte -- 11111111b<li/>
     * <li>{@code bitLength = 12} results in 2 bytes -- 00001111b, 11111111b<li/>
     * </ul>
     * @param bitLength number of bits
     * @param container byte array that can support at least {@code bitLength} bits
     * @throws IllegalArgumentException if {@code bitLength <= 0}, or if {@code container} isn't big enough
     */
    private static void insertOneBits(int bitLength, byte[] container) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bitLength);
        
        int fullByteCount = bitLength / 8;
        int remainingBits = bitLength % 8;
        
        int byteLength = fullByteCount + (remainingBits == 0 ? 0 : 1);
        
        Validate.isTrue(container.length >= byteLength);

        for (int i = 0; i < fullByteCount; i++) {
            container[container.length - i - 1] = (byte) 0xFF;
        }
        
        if (remainingBits > 0) {
            byte partialByte = (byte) (0xFF >>> (8 - remainingBits));
            container[container.length - byteLength] = partialByte;
        }
        
        // 0 out the remaining bytes
        for (int i = 0; i < container.length - byteLength; i++) {
            container[i] = 0;
        }
    }
    
    private static int calculateRequiredByteArraySize(int bitLength) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bitLength);
        
        int fullByteCount = bitLength / 8;
        int remainingBits = bitLength % 8;
        
        int byteLength = fullByteCount + (remainingBits == 0 ? 0 : 1);
        
        return byteLength;
    }

    private static void clearUnusedTopBits(int bitLength, byte[] container) {
        // Clear unused top bits
        int partialBitCount = bitLength % 8;
        if (partialBitCount != 0) {
            // e.g. partialBitCount == 3, then clearBitMask 11111111b -> 00011111b -> 11111000b -> 00000111b
            int clearBitMask = ~((0xFF >>> partialBitCount) << partialBitCount);
            container[0] = (byte) (container[0] & clearBitMask);
        }
    }
    
    /**
     * XOR this ID with another ID and return the result as a new ID. The bit length of the IDs must match.
     * @param other other ID to XOR against
     * @return new ID that is {@code this ^ other}
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code this} doesn't match the limit from {@code other}
     */
    public Id xor(Id other) {
        Validate.notNull(other);
        Validate.isTrue(bitLength == other.bitLength);

        byte[] xoredData = new byte[data.length]; // this and other have data field of same size if bitLength is same... checked above
        
        for (int i = 0; i < xoredData.length; i++) {
            xoredData[i] = (byte) ((xoredData[i] ^ other.data[i]) & 0xFF); // is 0xFF nessecary? -- yes due to byte to int upcast?
        }
        
        Id xoredId = new Id(xoredData, bitLength);

        return xoredId;
    }

    /**
     * Get the number of bits that are the same between this ID and another ID
     * @param other other ID to test against
     * @return number of common prefix bits
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the limit from {@code this} doesn't match the limit from {@code other}
     */
    public int getSharedPrefixLength(Id other) {
        Validate.notNull(other);
        Validate.isTrue(bitLength == other.bitLength);

        int byteMatchCount = 0;
        for (int i = data.length - 1; i >= 0; i++) {
            if (other.data[i] != data[i]) {
                break;
            }
            byteMatchCount++;
        }
        
        int nextByteIdx = data.length - byteMatchCount + 1;
        int thisLastByte = data[nextByteIdx] & 0xFF;
        int otherLastByte = other.data[nextByteIdx] & 0xFF;
        
        int bitMatchCount = 0;
        for (int i = 7; i >= 0; i++) {
            int thisBit = thisLastByte >> i;
            int otherBit = otherLastByte >> i;
            if (thisBit == otherBit) {
                break;
            }
            bitMatchCount++;
        }
        
        int unusedBitsInId = 8 - (bitLength % 8);
        int finalBitMatchCount = (byteMatchCount * 8) - unusedBitsInId + bitMatchCount;
        
        return finalBitMatchCount;
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
        Validate.isTrue(index < bitLength);
        
        int bitPos = index % 8;
        int bytePos = index / 8;
        
        int bitMask = 1 << (7 - bitPos);
        return (data[bytePos] & bitMask) != 0;
    }

    /**
     * Set bit within a copy of this ID.
     * @param index index of bit
     * @param bit {@code true} if bit is 1, {@code false} if bit is 0
     * @return new ID that has bit set
     * @throws IllegalArgumentException if {@code index < 0} or if {@code index > bitLength}
     */
    public Id setBit(int index, boolean bit) {
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < bitLength);
        
        byte[] dataCopy = Arrays.copyOf(data, data.length);
        
        int bitPos = index % 8;
        int bytePos = index / 8;
        
        if (bit) {
            int bitMask = 1 << (7 - bitPos);
            dataCopy[bytePos] |= bitMask;
        } else {
            int bitMask = ~(1 << (7 - bitPos));
            dataCopy[bytePos] &= bitMask;
        }
        
        clearUnusedTopBits(bitLength, dataCopy); // not nessecary but just incase
        return new Id(dataCopy, bitLength);
    }

    /**
     * Gets the maximum bit length for this ID.
     * @return max bit length for ID
     */
    public int getBitLength() {
        return bitLength;
    }

    /**
     * Gets a copy of the data for this ID. You can convert this to a BigInteger via {@code new BigInteger(1, getData())}.
     * @return copy of the ID data
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Arrays.hashCode(this.data);
        hash = 89 * hash + this.bitLength;
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
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (this.bitLength != other.bitLength) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Id{" + "data=" + Hex.encodeHexString(data) + ", bitLength=" + bitLength + '}';
    }
}