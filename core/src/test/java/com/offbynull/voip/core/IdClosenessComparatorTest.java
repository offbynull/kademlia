package com.offbynull.voip.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class IdClosenessComparatorTest {

    private IdClosenessComparator fixture = new IdClosenessComparator(Id.createFromLong(0x0L, 3)); // 000
    
    @Test
    public void mustIdentifyWhenEqual() {
        Id o1 = Id.createFromLong(0x1L, 3); // 001
        
        int res = fixture.compare(o1, o1);
        assertEquals(0, res);
    }

    @Test
    public void mustIdentifyByLargerPrefix() {
        Id o1 = Id.createFromLong(0x1L, 3); // 001
        Id o2 = Id.createFromLong(0x3L, 3); // 011
        
        int res1 = fixture.compare(o1, o2);
        assertEquals(1, res1);
        int res2 = fixture.compare(o2, o1);
        assertEquals(-1, res2);
    }
    
    @Test
    public void mustFlipBitsToIdentifyWhenArgumentsHaveEqualPrefix() {
        Id o1 = Id.createFromLong(0x6L, 3); // 110
        Id o2 = Id.createFromLong(0x7L, 3); // 111
        
        int res1 = fixture.compare(o1, o2);
        assertEquals(1, res1);
        int res2 = fixture.compare(o2, o1);
        assertEquals(-1, res2);
    }
    
}
