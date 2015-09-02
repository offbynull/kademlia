package com.offbynull.voip.core;

import static org.junit.Assert.assertArrayEquals;
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
        BitString bitString1 = BitString.createFromNumber(0x1FFFF00000000000L, 0, 32);
        BitString bitString2 = BitString.create(new byte[] { 0x1F, (byte) 0xFF, (byte) 0xF0, 0x00 }, 0, 32);
        
        assertEquals(bitString1, bitString2);
    }

    @Test
    public void mustIgnoreUnusedBitsWhenConstructing() {
        BitString bitString1 = BitString.createFromNumber(0xABCD8CF000000000L, 4, 8);
        BitString bitString2 = BitString.create(new byte[] { (byte) 0xAD, (byte) 0xC8, (byte) 0x0F }, 0, 8);
        
        assertEquals(bitString1, bitString2);
    }

    @Test
    public void mustFailWhenConstructingWhenOffsetOutOfBounds() {
        expectedException.expect(IllegalArgumentException.class);
        BitString.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 32, 0);
    }

    @Test
    public void mustFailWhenConstructingWhenLengthOutOfBounds() {
        expectedException.expect(IllegalArgumentException.class);
        BitString.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 0, 33);
    }
    
    @Test
    public void mustGetIndividualBits() {
        BitString bitString = BitString.createFromNumber(0x3C5A000000000000L, 0, 16);

        assertFalse(bitString.getBit(0));
        assertFalse(bitString.getBit(1));
        assertTrue(bitString.getBit(2));
        assertTrue(bitString.getBit(3));

        assertTrue(bitString.getBit(4));
        assertTrue(bitString.getBit(5));
        assertFalse(bitString.getBit(6));
        assertFalse(bitString.getBit(7));

        assertFalse(bitString.getBit(8));
        assertTrue(bitString.getBit(9));
        assertFalse(bitString.getBit(10));
        assertTrue(bitString.getBit(11));

        assertTrue(bitString.getBit(12));
        assertFalse(bitString.getBit(13));
        assertTrue(bitString.getBit(14));
        assertFalse(bitString.getBit(15));
    }

    @Test
    public void mustGetGroupsOfBits() {
        BitString bitString = BitString.createFromNumber(0x3C5A000000000000L, 0, 16);
        
        BitString expected = BitString.createFromNumber(0x5000000000000000L, 3, 4);
        BitString actual = bitString.getBits(8, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void mustGetGroupsOfBitsAsLong() {
        BitString bitString = BitString.createFromNumber(0x3CFA000000000000L, 0, 16);
        
        long expected = 0xC5L;
        long actual = bitString.getBitsAsLong(4, 8);

        assertEquals(expected, actual);
    }

    @Test
    public void mustSetIndividualBits() {
        BitString bitString = BitString.createFromNumber(0x0000000000000000L, 48, 16);

        bitString = bitString.setBit(0, false);
        bitString = bitString.setBit(1, false);
        bitString = bitString.setBit(2, true);
        bitString = bitString.setBit(3, true);

        bitString = bitString.setBit(4, true);
        bitString = bitString.setBit(5, true);
        bitString = bitString.setBit(6, false);
        bitString = bitString.setBit(7, false);

        bitString = bitString.setBit(8, false);
        bitString = bitString.setBit(9, true);
        bitString = bitString.setBit(10, false);
        bitString = bitString.setBit(11, true);

        bitString = bitString.setBit(12, true);
        bitString = bitString.setBit(13, false);
        bitString = bitString.setBit(14, true);
        bitString = bitString.setBit(15, false);
        
        assertEquals(BitString.createFromNumber(0x0000000000003C5AL, 48, 16), bitString);
    }

    @Test
    public void mustSetGroupsOfBitsTo1() {
        BitString bitString = BitString.createFromNumber(0x3C5A000000000000L, 0, 16);
        BitString modifier  = BitString.createFromNumber(0xF000000000000000L, 4, 4);
        
        BitString actual = bitString.setBits(8, modifier);
        BitString expected = BitString.createFromNumber(0x3C5F000000000000L, 0, 16);

        assertEquals(expected, actual);
    }

    @Test
    public void mustSetGroupsOfBitsTo0() {
        BitString bitString = BitString.createFromNumber(0x3C5A000000000000L, 0, 16);
        BitString modifier  = BitString.createFromNumber(0x0000000000000000L, 4, 4);
        
        BitString actual = bitString.setBits(8, modifier);
        BitString expected = BitString.createFromNumber(0x3C50000000000000L, 0, 16);

        assertEquals(expected, actual);
    }
    
    @Test
    public void mustFlipBits() {
        BitString bitString = BitString.createFromNumber(0x3C5AL, 48, 16);

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
        
        assertEquals(BitString.createFromNumber(0xC3A5L, 48, 16), bitString);
    }
    
    @Test
    public void mustIdentifyCommonPrefixLength() {
        BitString baseBitString = BitString.createFromNumber(0x000000000000A2F0L, 48, 12);
        BitString noMatchBitString = BitString.createFromNumber(0x0000000000000000L, 48, 12);
        BitString partialMatchBitString = BitString.createFromNumber(0x000000000000A300L, 48, 12);
        BitString fullMatchBitString = BitString.createFromNumber(0x000000000000A2F0L, 48, 12);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString));
        assertEquals(12, baseBitString.getSharedPrefixLength(fullMatchBitString));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnSmallerSizes() {
        BitString baseBitString = BitString.createFromNumber(0x000000000000A2F0L, 48, 12);
        BitString noMatchBitString = BitString.createFromNumber(0x0000000000000000L, 48, 1);
        BitString partialMatchBitString1 = BitString.createFromNumber(0x000000000000A300L, 48, 9);
        BitString partialMatchBitString2 = BitString.createFromNumber(0x000000000000A2C0L, 48, 9);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString1));
        assertEquals(9, baseBitString.getSharedPrefixLength(partialMatchBitString2));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnLargerSizes() {
        BitString baseBitString = BitString.createFromNumber(0x000000000000A2F0L, 48, 12);
        BitString noMatchBitString = BitString.createFromNumber(0x0000000000000000L, 48, 16);
        BitString partialMatchBitString1 = BitString.createFromNumber(0x000000000000A300L, 48, 16);
        BitString partialMatchBitString2 = BitString.createFromNumber(0x000000000000A220L, 48, 16);
        
        assertEquals(0, baseBitString.getSharedPrefixLength(noMatchBitString));
        assertEquals(7, baseBitString.getSharedPrefixLength(partialMatchBitString1));
        assertEquals(10, baseBitString.getSharedPrefixLength(partialMatchBitString2));
    }
}
