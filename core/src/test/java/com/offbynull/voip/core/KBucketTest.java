package com.offbynull.voip.core;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class KBucketTest {
    
    private static final Id BASE_ID = Id.createFromLong(0x12340000L, 32);
    
    private static final Node NODE_0000 = new Node(Id.createFromLong(0x12340000L, 32), "0");
    private static final Node NODE_0001 = new Node(Id.createFromLong(0x12341000L, 32), "1");
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x12342000L, 32), "2");
    private static final Node NODE_0011 = new Node(Id.createFromLong(0x12343000L, 32), "3");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x12344000L, 32), "4");
    private static final Node NODE_0101 = new Node(Id.createFromLong(0x12345000L, 32), "5");
    private static final Node NODE_0110 = new Node(Id.createFromLong(0x12346000L, 32), "6");
    private static final Node NODE_0111 = new Node(Id.createFromLong(0x12347000L, 32), "7");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x12348000L, 32), "8");
    private static final Node NODE_1001 = new Node(Id.createFromLong(0x12349000L, 32), "9");
    private static final Node NODE_1010 = new Node(Id.createFromLong(0x1234A000L, 32), "A");
    private static final Node NODE_1011 = new Node(Id.createFromLong(0x1234B000L, 32), "B");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x1234C000L, 32), "C");
    private static final Node NODE_1101 = new Node(Id.createFromLong(0x1234D000L, 32), "D");
    private static final Node NODE_1110 = new Node(Id.createFromLong(0x1234E000L, 32), "E");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x1234F000L, 32), "F");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private KBucket fixture = new KBucket(BASE_ID, BASE_ID.getBitString().getBits(0, 16), 4, 3); // 16 bit prefix, 4 bucket cap, 3 cache cap
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustSplitInTo1Bucket() throws Throwable { // why would anyone want to do this? it's just a copy
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1101);
        
        KBucket[] buckets = fixture.split(0);
        
        assertEquals(1, buckets.length);
        
        assertEquals(fixture.dumpBucket(), buckets[0].dumpBucket());
        assertEquals(fixture.dumpCache(), buckets[0].dumpCache());
        assertEquals(fixture.getLastUpdateTime(), buckets[0].getLastUpdateTime());
    }

    @Test
    public void mustSplitInTo2Buckets() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1101);
        
        KBucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(NODE_0010, buckets[0].dumpBucket().get(0).getNode());
        assertEquals(NODE_0100, buckets[0].dumpBucket().get(1).getNode());
        assertEquals(NODE_1000, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_1100, buckets[1].dumpBucket().get(1).getNode());
        assertEquals(NODE_1110, buckets[1].dumpBucket().get(2).getNode());
        assertEquals(NODE_1101, buckets[1].dumpBucket().get(3).getNode());
        
        assertEquals(0, buckets[0].dumpCache().size());
        assertEquals(1, buckets[1].dumpCache().size());
        assertEquals(NODE_1111, buckets[1].dumpCache().get(0).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(3L));
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(7L));
    }

    @Test
    public void mustSplitInTo2BucketsWhereBucket0IsEmpty() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_1101);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1111);
        
        KBucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(0, buckets[0].dumpBucket().size());
        assertEquals(0, buckets[0].dumpCache().size());
        assertEquals(4, buckets[1].dumpBucket().size());
        assertEquals(3, buckets[1].dumpCache().size());
        
        assertEquals(NODE_1000, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_1001, buckets[1].dumpBucket().get(1).getNode());
        assertEquals(NODE_1010, buckets[1].dumpBucket().get(2).getNode());
        assertEquals(NODE_1011, buckets[1].dumpBucket().get(3).getNode());
        
        assertEquals(NODE_1101, buckets[1].dumpCache().get(0).getNode());
        assertEquals(NODE_1110, buckets[1].dumpCache().get(1).getNode());
        assertEquals(NODE_1111, buckets[1].dumpCache().get(2).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), Instant.MIN);
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(7L));
    }

    @Test
    public void mustSplitInTo4Buckets() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001); // goes in to bucket
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0101);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1001);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1101);

        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0011); // goes in to cache
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1011);
        
        KBucket[] buckets = fixture.split(2);
        
        assertEquals(4, buckets.length);
        
        assertEquals(2, buckets[0].dumpBucket().size());
        assertEquals(0, buckets[0].dumpCache().size());
        assertEquals(2, buckets[1].dumpBucket().size());
        assertEquals(0, buckets[1].dumpCache().size());
        assertEquals(2, buckets[2].dumpBucket().size());
        assertEquals(0, buckets[2].dumpCache().size());
        assertEquals(1, buckets[3].dumpBucket().size());
        assertEquals(0, buckets[3].dumpCache().size());
        
        assertEquals(NODE_0001, buckets[0].dumpBucket().get(0).getNode());
        assertEquals(NODE_0011, buckets[0].dumpBucket().get(1).getNode());

        assertEquals(NODE_0101, buckets[1].dumpBucket().get(0).getNode());
        assertEquals(NODE_0111, buckets[1].dumpBucket().get(1).getNode());

        assertEquals(NODE_1001, buckets[2].dumpBucket().get(0).getNode());
        assertEquals(NODE_1011, buckets[2].dumpBucket().get(1).getNode());

        assertEquals(NODE_1101, buckets[3].dumpBucket().get(0).getNode());
        
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(5L));
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(6L));
        assertEquals(buckets[2].getLastUpdateTime(), BASE_TIME.plusMillis(7L));
        assertEquals(buckets[3].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }
    
}
