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
        // IDs must be xxxxxxxx xxxxxxx1 11111111 11111111
        Id id1 = Id.createContiguous(17, 32);
        Id id2 = Id.createFromInteger(0x1FFFFL, 32);
        Id id3 = Id.create(new byte[] { 0x01, (byte) 0xFF, (byte) 0xFF }, 32);
        
        assertEquals(id1, id2);
        assertEquals(id2, id3);
        assertEquals(id1, id3);
    }

    @Test
    public void mustIgnoreUnusedBitsWhenConstructing() {
        // IDs must be xxxxxx01 11111111 11111111
        Id id1 = Id.createContiguous(17, 18);
        Id id2 = Id.createFromInteger(0xA9FFFFL, 18);
        Id id3 = Id.create(new byte[] { (byte) 0xFD, (byte) 0xFF, (byte) 0xFF }, 18);
        
        assertEquals(id1, id2);
        assertEquals(id2, id3);
        assertEquals(id1, id3);
    }
    
    @Test
    public void mustFailWhenConstructingTooLargeContiguousId() {
        expectedException.expect(IllegalArgumentException.class);
        Id.createContiguous(19, 18);
    }

    @Test
    public void mustFailWhenConstructingTooLargeId() {
        expectedException.expect(IllegalArgumentException.class);
        Id.create(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF }, 18);
    }
    
    @Test
    public void mustGetBits() {
        Id id = Id.createFromInteger(0x3C5AL, 16);

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
        Id id = Id.createFromInteger(0, 16);

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
        
        assertEquals(Id.createFromInteger(0x3C5AL, 16), id);
    }
}
