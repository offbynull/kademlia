package com.offbynull.voip.core;

import org.apache.commons.lang3.Validate;

final class InternalUtils {
    private InternalUtils() {
        // do nothing
    }
    
    // Generates an array of bitstrings of 2^bitCount elements, where each element i is prefix + i.
    //
    // So for example, if prefix = 1010 and bitCount = 2, the returning array would contain ...
    //
    // ret[0] = 1010 00   note that 0 = 00
    // ret[1] = 1010 01   note that 1 = 01
    // ret[2] = 1010 10   note that 2 = 10
    // ret[3] = 1010 11   note that 3 = 11
    //
    // note that bitCount = 2, 2^2 = 4, which results in 4 elements
    public static BitString[] appendToBitString(BitString prefix, int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to create in to 2^30 bitstrings, but
                                         // whatever we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new BitString[1 << 31] -- 1 << 31 is negative
                                         // new BitString[1 << 30] -- 1 << 30 is positive

        // Create bitstrings
        int len = 1 << bitCount;
        BitString[] ret = new BitString[len];
        for (int num = 0; num < ret.length; num++) {
            BitString numAsBitString = toBitString(num, bitCount);
            BitString appendedBitString = prefix.appendBits(numAsBitString);
            ret[num] = appendedBitString;
        }
        
        return ret;
    }
    
    // The int {@code 0xABCD} with a bitlength of 12 would result in the bit string {@code 10 1011 1100 1101}.
    // Bit     15 14 13 12   11 10 09 08   07 06 05 04   03 02 01 00
    //         ------------------------------------------------------
    //         1  0  1  0    1  0  1  1    1  1  0  0    1  1  0  1
    //         A             B             C             D
    //               ^                                            ^
    //               |                                            | 
    //             start                                         end
    private static BitString toBitString(int data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);

        data = data << (32 - bitLength);
        return BitString.createReadOrder(toBytes(data), 0, bitLength);
    }
    
    private static byte[] toBytes(int data) { // returns in big endian format
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int shiftAmount = 24 - (i * 4);
            bytes[i] = (byte) (data >>> shiftAmount);
        }
        return bytes;
    }
}
