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
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

/**
 * Bit string.
 * <p>
 * Class is immutable.
 * @author Kasra Faghihi
 */
public final class BitString implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] data;
    private final int bitLength;
    
    // make sure that whatever you pass in as data is a copy / not-shared.
    private BitString(byte[] data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);
        
        int minLength = calculateRequiredByteArraySize(bitLength);
        Validate.isTrue(data.length == minLength);
        
        this.data = data;
        this.bitLength = bitLength;
    }

    /**
     * Constructs a {@link BitString} from a byte array.
     * @param data bitstring value
     * @param bitLength number of bits in this bitstring
     * @return created bitstring
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code bitLength <= 0}, or if {@code data} is larger than the minimum number of bytes that it
     * takes to retain {@code bitLength} (e.g. if you're retaining 12 bits, you need 2 bytes or less -- {@code 12/8 + (12%8 == 0 ? 0 : 1)})
     */
    public static BitString create(byte[] data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);
        
        int length = calculateRequiredByteArraySize(bitLength);
        Validate.isTrue(data.length <= length);
        
        // Create copy. Copy is of size length. If data is less than length, extra 0's are added as prefix.
        byte[] dataCopy = new byte[length];
        System.arraycopy(data, 0, dataCopy, 0, data.length);
        
        clearUnusedTailBits(bitLength, dataCopy);
        return new BitString(dataCopy, bitLength);
    }
    
    
    /**
     * Constructs a {@link BitString} from a long. Long is read in big-endian format.
     * <p>
     * For example, {@code createFromLong(0xA700000000000000L, 5)} would create a bitstring of {@code 1010 1}.
     * @param data bitstring value
     * @param bitLength number of total bits allowed in the bitstring, starting from the top of bit of {@code data}
     * @return created bitstring
     * @throws IllegalArgumentException if {@code 64 < bitLength < 1}
     */
    public static BitString createFromLong(long data, int bitLength) {
        Validate.isTrue(bitLength > 0);
        Validate.isTrue(bitLength < 64);
        
        int length = calculateRequiredByteArraySize(bitLength);
        
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            int insertIntoIdx = i;
            int shiftRightAmount = 56 - (i * 8);
            bytes[insertIntoIdx] = (byte) (data >>> shiftRightAmount);
        }
        
        clearUnusedTailBits(bitLength, bytes);
        return new BitString(bytes, bitLength);
    }
    
    private static int calculateRequiredByteArraySize(int bitLength) {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, bitLength);
        
        int fullByteCount = bitLength / 8;
        int remainingBits = bitLength % 8;
        
        int byteLength = fullByteCount + (remainingBits == 0 ? 0 : 1);
        
        return byteLength;
    }

    private static void clearUnusedTailBits(int bitLength, byte[] container) {
        // Clear unused top bits
        int partialBitCount = bitLength % 8;
        if (partialBitCount != 0) {
            // e.g. partialBitCount == 3, then clearBitMask 1..1 1000 0000b -> 1..1 1110 0000b -> 0..0 1110 0000b
            int lastIdx = container.length - 1;
            int clearBitMask = (0xFFFFFF80 >> (partialBitCount - 1)) & 0xFF;
            container[lastIdx] = (byte) (container[lastIdx] & clearBitMask);
        }
    }

    // UNTESTED, but is this even required?
//    /**
//     * XOR this bitstring with another bitstring and return the result as a new bitstring. The bit length of the bitstrings must match.
//     * @param other other bitstring to XOR against
//     * @return new bitstring that is {@code this ^ other}
//     * @throws NullPointerException if any argument is {@code null}
//     * @throws IllegalArgumentException if the limit from {@code this} doesn't match the limit from {@code other}
//     */
//    public BitString xor(BitString other) {
//        Validate.notNull(other);
//        Validate.isTrue(bitLength == other.bitLength);
//
//        byte[] xoredData = new byte[data.length]; // this and other have data field of same size if bitLength is same... checked above
//        
//        for (int i = 0; i < xoredData.length; i++) {
//            xoredData[i] = (byte) ((xoredData[i] ^ other.data[i]) & 0xFF); // is 0xFF nessecary? -- yes due to byte to int upcast?
//        }
//        
//        clearUnusedTopBits(bitLength, xoredData);
//        BitString xored = new BitString(xoredData, bitLength);
//
//        return xored;
//    }

    // UNTESTED, but is this even required?
//    public boolean startsWith(BitString other) {
//        Validate.notNull(other);
//        
//        int sharedPrefixLength = getSharedPrefixLength(other);
//        return sharedPrefixLength == other.bitLength;
//    }
    
    /**
     * Get the number of bits that are the same between this bitstring and another bitstring.
     * @param other other bitstring to test against
     * @return number of common prefix bits
     * @throws NullPointerException if any argument is {@code null}
     */
    public int getSharedPrefixLength(BitString other) {
        Validate.notNull(other);
        
        int maxCompareLenAsBits = Math.min(bitLength, other.bitLength);
        int maxCompareLenAsBytes = calculateRequiredByteArraySize(maxCompareLenAsBits);
        
        
        int nextByteIdx = 0;
        for (int i = 0; i < maxCompareLenAsBytes; i++) {
            if (other.data[i] != data[i]) {
                break;
            }
            nextByteIdx++;
        }
        
        if (nextByteIdx == maxCompareLenAsBytes) {
            // All bytes matched, both strings match entirely
            return maxCompareLenAsBits;
        }
        
        int thisLastByte = data[nextByteIdx] & 0xFF;
        int otherLastByte = other.data[nextByteIdx] & 0xFF;
      
        int bitMatchCount = 0;
        for (int i = 7; i >= 0; i--) {
            int thisBit = thisLastByte >> i;
            int otherBit = otherLastByte >> i;
            if (thisBit != otherBit) {
                break;
            }
            bitMatchCount++;
        }
        
        int finalBitMatchCount = (nextByteIdx * 8) + bitMatchCount;
        
        return finalBitMatchCount;
    }
    
    /**
     * Get bit from this bitstring. Bit 0 is the left-most bit, bit 1 the second left-most bit, etc. For example, bitstring 3C...
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
     * Set bit within a copy of this bitstring. Bit 0 is the left-most bit, bit 1 the second left-most bit, etc. For example, bitstring
     * 3C...
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
    public BitString setBit(int index, boolean bit) {
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
        
        clearUnusedTailBits(bitLength, dataCopy); // not nessecary but just incase
        return new BitString(dataCopy, bitLength);
    }

    /**
     * Flip bit within a copy of this bitstring.
     * @param index index of bit
     * @return new bitstring that has bit flipped
     * @throws IllegalArgumentException if {@code index < 0} or if {@code index > bitLength}
     */
    public BitString flipBit(int index) {
        Validate.isTrue(index >= 0);
        Validate.isTrue(index < bitLength);
        
        boolean bit = getBit(index);
        return setBit(index, !bit);
    }

    /**
     * Gets the maximum bit length for this bitstring.
     * @return max bit length for bitstring
     */
    public int getBitLength() {
        return bitLength;
    }

    /**
     * Gets a copy of the data for this bitstring. You can convert this to an unsigned BigInteger via {@code new BigInteger(1, getData())}.
     * @return copy of the bitstring data
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
        final BitString other = (BitString) obj;
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
        return "BitString{" + "data=" + Hex.encodeHexString(data) + ", bitLength=" + bitLength + '}';
    }
}