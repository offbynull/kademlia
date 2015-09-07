package com.offbynull.voip.core;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class KBucketTest {
    
    private static final Id BASE_ID = Id.createFromLong(0x12340000L, 32);
    
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x12342000L, 32), "1");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x12344000L, 32), "2");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x12348000L, 32), "3");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x1234C000L, 32), "4");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x1234F000L, 32), "5");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private KBucket fixture = new KBucket(BASE_ID, BASE_ID.getBitString().getBits(0, 16), 32, 4); // prefix of 16 bits, capacity of 4
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustSplitInTo2Buckets() {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        
        KBucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(NODE_0010, buckets[0].dumpBucket().get(0).getNode());
        assertEquals(NODE_0100, buckets[0].dumpBucket().get(1).getNode());
        assertEquals(NODE_1000, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_1100, buckets[1].dumpBucket().get(1).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(3L));
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }

    @Test
    public void mustSplitInTo2BucketsWhere1BucketIsEmpty() {
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1111);
        
        KBucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(NODE_1000, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_1100, buckets[1].dumpBucket().get(1).getNode());
        assertEquals(NODE_1111, buckets[1].dumpBucket().get(2).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), Instant.MIN);
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }

    @Test
    public void mustSplitInTo4Buckets() {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        
        KBucket[] buckets = fixture.split(2);
        
        assertEquals(4, buckets.length);
        
        assertEquals(NODE_0010, buckets[0].dumpBucket().get(0).getNode());
        assertEquals(NODE_0100, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_1000, buckets[2].dumpBucket().get(0).getNode());
        assertEquals(NODE_1100, buckets[3].dumpBucket().get(0).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(1L));
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(3L));
        assertEquals(buckets[2].getLastUpdateTime(), BASE_TIME.plusMillis(2L));
        assertEquals(buckets[3].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }
    
}
