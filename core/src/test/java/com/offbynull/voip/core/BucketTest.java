package com.offbynull.voip.core;

import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BucketTest {
    
    
    private Id baseId = Id.createFromInteger(0x1234ABCDL, 32);
    private Bucket fixture = new Bucket(baseId, 16, 4); // bucket for prefix of 4, bucket capacity of 4

    @Test
    public void mustInsertAndRetrieveNodes() {
        Bucket.TouchResult touchRes;
        
        Instant time1 = Instant.ofEpochMilli(0L);
        Instant time2 = Instant.ofEpochMilli(1L);
        Node node1 = new Node(Id.createFromInteger(0x12340000L, 32), "0");
        Node node2 = new Node(Id.createFromInteger(0x12340001L, 32), "0");
        
        touchRes = fixture.touch(time1, node1);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        touchRes = fixture.touch(time2, node2);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        assertEquals(node1, fixture.get(0));
        assertEquals(node2, fixture.get(1));
        assertEquals(2, fixture.size());
    }
    
}
