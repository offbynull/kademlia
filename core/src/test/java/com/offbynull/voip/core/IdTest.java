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
        Id id1 = Id.create(new byte[] { (byte) 0xF8, (byte) 0xFF, (byte) 0x0F, 0x00 }, 32);
        Id id2 = Id.createFromNumber(0xF8FF0F00L, 32);
        
        assertEquals(id1, id2);
    }

    @Test
    public void mustFailConstructingWhenLengthOutOfBoundsForByteArray() {
        expectedException.expect(IllegalArgumentException.class);
        Id.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, }, 33);
    }

    @Test
    public void mustFailConstructingWhenLengthOutOfBoundsForLong() {
        expectedException.expect(IllegalArgumentException.class);
        Id.createFromNumber(0x000FFFFF12345678L, 65);
    }

    @Test
    public void mustGetBits() {
        Id id = Id.createFromNumber(0x3C5AL, 16);
        
        long expected = 0x5L;
        long actual = id.getBitsAsLong(8, 4);

        assertEquals(expected, actual);
    }

    @Test
    public void mustSetBitsTo1() {
        Id id = Id.createFromNumber(0x3C5AL, 16);
        long modifier = 0x0FL;
        
        Id actual = id.setBitsAsLong(modifier, 6, 4);
        Id expected = Id.createFromNumber(0x3FDAL, 16);

        assertEquals(expected, actual);
    }

    @Test
    public void mustSetBitsTo0() {
        Id id = Id.createFromNumber(0x3C5AL, 16);
        long modifier = 0x00L;
        
        Id actual = id.setBitsAsLong(modifier, 6, 4);
        Id expected = Id.createFromNumber(0x3C1AL, 16);

        assertEquals(expected, actual);
    }

    @Test
    public void mustFlipBits() {
        Id id = Id.createFromNumber(0x3C5AL, 16);

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
        
        assertEquals(Id.createFromNumber(0xC3A5L, 16), id);
    }

    @Test
    public void mustIdentifyCommonPrefixLength() {
        Id baseId = Id.createFromNumber(0xA2FL, 12);
        Id noMatchId = Id.createFromNumber(0x000L, 12);
        Id partialMatchId = Id.createFromNumber(0xA30L, 12);
        Id fullMatchId = Id.createFromNumber(0xA2FL, 12);
        
        assertEquals(0, baseId.getSharedPrefixLength(noMatchId));
        assertEquals(7, baseId.getSharedPrefixLength(partialMatchId));
        assertEquals(12, baseId.getSharedPrefixLength(fullMatchId));
    }
}
