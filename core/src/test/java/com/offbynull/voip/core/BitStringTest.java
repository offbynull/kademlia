package com.offbynull.voip.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BitStringTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustCreateTheSameBitStringUsingAllConstructors() {
        BitString bitString1 = BitString.createFromLong(0x1FFFF00000000000L, 32);
        BitString bitString2 = BitString.create(new byte[] { 0x1F, (byte) 0xFF, (byte) 0xF0 }, 32);
        
        assertEquals(bitString1, bitString2);
    }

    @Test
    public void mustIgnoreUnusedBitsWhenConstructing() {
        BitString bitString1 = BitString.createFromLong(0xFFFF8C0000000000L, 18);
        BitString bitString2 = BitString.create(new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xBF }, 18);
        
        assertEquals(bitString1, bitString2);
    }

    @Test
    public void mustFailWhenConstructingTooLargeBitString() {
        expectedException.expect(IllegalArgumentException.class);
        BitString.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 18);
    }
    
    @Test
    public void mustGetBits() {
        BitString bitString = BitString.createFromLong(0x3C5A000000000000L, 16);

        // 3
        assertFalse(bitString.getBit(0));
        assertFalse(bitString.getBit(1));
        assertTrue(bitString.getBit(2));
        assertTrue(bitString.getBit(3));
        
        // 9
        assertTrue(bitString.getBit(4));
        assertTrue(bitString.getBit(5));
        assertFalse(bitString.getBit(6));
        assertFalse(bitString.getBit(7));
        
        // 5
        assertFalse(bitString.getBit(8));
        assertTrue(bitString.getBit(9));
        assertFalse(bitString.getBit(10));
        assertTrue(bitString.getBit(11));

        // A
        assertTrue(bitString.getBit(12));
        assertFalse(bitString.getBit(13));
        assertTrue(bitString.getBit(14));
        assertFalse(bitString.getBit(15));
    }

    @Test
    public void mustSetBits() {
        BitString bitString = BitString.createFromLong(0, 16);

        // 3
        bitString = bitString.setBit(0, false);
        bitString = bitString.setBit(1, false);
        bitString = bitString.setBit(2, true);
        bitString = bitString.setBit(3, true);
        
        // 9
        bitString = bitString.setBit(4, true);
        bitString = bitString.setBit(5, true);
        bitString = bitString.setBit(6, false);
        bitString = bitString.setBit(7, false);
        
        // 5
        bitString = bitString.setBit(8, false);
        bitString = bitString.setBit(9, true);
        bitString = bitString.setBit(10, false);
        bitString = bitString.setBit(11, true);

        // A
        bitString = bitString.setBit(12, true);
        bitString = bitString.setBit(13, false);
        bitString = bitString.setBit(14, true);
        bitString = bitString.setBit(15, false);
        
        assertEquals(BitString.createFromLong(0x3C5A000000000000L, 16), bitString);
    }
    
    @Test
    public void mustFlipBits() {
        BitString bitString = BitString.createFromLong(0x3C5A000000000000L, 16);

        // 3
        bitString = bitString.flipBit(0);
        bitString = bitString.flipBit(1);
        bitString = bitString.flipBit(2);
        bitString = bitString.flipBit(3);
        
        // 9
        bitString = bitString.flipBit(4);
        bitString = bitString.flipBit(5);
        bitString = bitString.flipBit(6);
        bitString = bitString.flipBit(7);
        
        // 5
        bitString = bitString.flipBit(8);
        bitString = bitString.flipBit(9);
        bitString = bitString.flipBit(10);
        bitString = bitString.flipBit(11);

        // A
        bitString = bitString.flipBit(12);
        bitString = bitString.flipBit(13);
        bitString = bitString.flipBit(14);
        bitString = bitString.flipBit(15);
        
        assertEquals(BitString.createFromLong(0xC3A5000000000000L, 16), bitString);
    }
    
    @Test
    public void mustIdentifyCommonPrefixLengthOnUnaligned() {
        BitString baseBitString = BitString.createFromLong(0xA2F0000000000000L, 12);
        BitString noMatchBitString = BitString.createFromLong(0x0000000000000000L, 12);
        BitString partialMatchBitString = BitString.createFromLong(0xA300000000000000L, 12);
        BitString fullMatchBitString = BitString.createFromLong(0xA2F0000000000000L, 12);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString));
        assertEquals(12, baseBitString.getSharedPrefixLength(fullMatchBitString));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnUnalignedAndSmallerSizes() {
        BitString baseBitString = BitString.createFromLong(0xA2F0000000000000L, 12);
        BitString noMatchBitString = BitString.createFromLong(0x0000000000000000L, 1);
        BitString partialMatchBitString1 = BitString.createFromLong(0xA300000000000000L, 9);
        BitString partialMatchBitString2 = BitString.createFromLong(0xA2C0000000000000L, 9);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString1));
        assertEquals(9, baseBitString.getSharedPrefixLength(partialMatchBitString2));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnUnalignedAndLargerSizes() {
        BitString baseBitString = BitString.createFromLong(0xA2F0000000000000L, 12);
        BitString noMatchBitString = BitString.createFromLong(0x0000000000000000L, 16);
        BitString partialMatchBitString1 = BitString.createFromLong(0xA300000000000000L, 16);
        BitString partialMatchBitString2 = BitString.createFromLong(0xA2C0000000000000L, 16);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString1));
        assertEquals(10, baseBitString.getSharedPrefixLength(partialMatchBitString2));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnAligned() {
        BitString baseBitString = BitString.createFromLong(0xABCD000000000000L, 32);
        BitString noMatchBitString = BitString.createFromLong(0x0000000000000000L, 32);
        BitString partialMatchBitString = BitString.createFromLong(0xABCDFFFF00000000L, 32);
        BitString fullMatchBitString = BitString.createFromLong(0xABCD000000000000L, 32);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(16, baseBitString.getSharedPrefixLength(partialMatchBitString));
        assertEquals(32, baseBitString.getSharedPrefixLength(fullMatchBitString));
    }
}
