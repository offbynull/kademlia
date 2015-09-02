package com.offbynull.voip.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IdTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustCreateTheSameIdUsingAllConstructors() {
        Id id1 = Id.createFromTopBitsOfLong(0x1FFFF00000000000L, 32);
        Id id2 = Id.create(new byte[] { 0x1F, (byte) 0xFF, (byte) 0xF0 }, 32);
        
        assertEquals(id1, id2);
    }

    @Test
    public void mustIgnoreUnusedBitsWhenConstructing() {
        Id id1 = Id.createFromTopBitsOfLong(0xFFFF800000000000L, 18);
        Id id2 = Id.create(new byte[] { (byte) 0xFD, (byte) 0xFF, (byte) 0xFF }, 18);
        
        assertEquals(id1, id2);
    }

    @Test
    public void mustFailWhenConstructingTooLargeId() {
        expectedException.expect(IllegalArgumentException.class);
        Id.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 18);
    }
    
    @Test
    public void mustGetBits() {
        Id id = Id.createFromTopBitsOfLong(0x3C5AL, 16);

        // 3
        assertFalse(id.getBit(0));
        assertFalse(id.getBit(1));
        assertTrue(id.getBit(2));
        assertTrue(id.getBit(3));
        
        // 9
        assertTrue(id.getBit(4));
        assertTrue(id.getBit(5));
        assertFalse(id.getBit(6));
        assertFalse(id.getBit(7));
        
        // 5
        assertFalse(id.getBit(8));
        assertTrue(id.getBit(9));
        assertFalse(id.getBit(10));
        assertTrue(id.getBit(11));

        // A
        assertTrue(id.getBit(12));
        assertFalse(id.getBit(13));
        assertTrue(id.getBit(14));
        assertFalse(id.getBit(15));
    }

    @Test
    public void mustSetBits() {
        Id id = Id.createFromTopBitsOfLong(0, 16);

        // 3
        id = id.setBit(0, false);
        id = id.setBit(1, false);
        id = id.setBit(2, true);
        id = id.setBit(3, true);
        
        // 9
        id = id.setBit(4, true);
        id = id.setBit(5, true);
        id = id.setBit(6, false);
        id = id.setBit(7, false);
        
        // 5
        id = id.setBit(8, false);
        id = id.setBit(9, true);
        id = id.setBit(10, false);
        id = id.setBit(11, true);

        // A
        id = id.setBit(12, true);
        id = id.setBit(13, false);
        id = id.setBit(14, true);
        id = id.setBit(15, false);
        
        assertEquals(Id.createFromTopBitsOfLong(0x3C5AL, 16), id);
    }
    
    @Test
    public void mustFlipBits() {
        Id id = Id.createFromTopBitsOfLong(0x3C5AL, 16);

        // 3
        id = id.flipBit(0);
        id = id.flipBit(1);
        id = id.flipBit(2);
        id = id.flipBit(3);
        
        // 9
        id = id.flipBit(4);
        id = id.flipBit(5);
        id = id.flipBit(6);
        id = id.flipBit(7);
        
        // 5
        id = id.flipBit(8);
        id = id.flipBit(9);
        id = id.flipBit(10);
        id = id.flipBit(11);

        // A
        id = id.flipBit(12);
        id = id.flipBit(13);
        id = id.flipBit(14);
        id = id.flipBit(15);
        
        assertEquals(Id.createFromTopBitsOfLong(0xC3A5L, 16), id);
    }
    
    @Test
    public void mustIdentifyCommonPrefixLengthOnUnaligned() {
        Id baseId = Id.createFromTopBitsOfLong(0xA2F, 12);
        Id noMatchId = Id.createFromTopBitsOfLong(0x000, 12);
        Id partialMatchId = Id.createFromTopBitsOfLong(0xA30, 12);
        Id fullMatchId = Id.createFromTopBitsOfLong(0xA2F, 12);
        
        assertEquals(0, baseId.getSharedPrefixLength(noMatchId));
        assertEquals(7, baseId.getSharedPrefixLength(partialMatchId));
        assertEquals(12, baseId.getSharedPrefixLength(fullMatchId));
    }

    @Test
    public void mustIdentifyCommonPrefixLengthOnAligned() {
        Id baseId = Id.createFromTopBitsOfLong(0xABCD0000L, 32);
        Id noMatchId = Id.createFromTopBitsOfLong(0x00000000L, 32);
        Id partialMatchId = Id.createFromTopBitsOfLong(0xABCDFFFFL, 32);
        Id fullMatchId = Id.createFromTopBitsOfLong(0xABCD0000L, 32);
        
        assertEquals(0, baseId.getSharedPrefixLength(noMatchId));
        assertEquals(16, baseId.getSharedPrefixLength(partialMatchId));
        assertEquals(32, baseId.getSharedPrefixLength(fullMatchId));
    }
}
