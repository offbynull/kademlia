package com.offbynull.voip.core;

import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BucketTest {
    
    
    private static final Id BASE_ID = Id.createFromLong(0x12340000L, 32);
    
    private static final Node NODE_0000 = new Node(Id.createFromLong(0x12340000L, 32), "1");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x12344000L, 32), "2");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x12348000L, 32), "3");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x1234C000L, 32), "4");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x1234F000L, 32), "5");
    
    private static final Node NODE_SHORT_PREFIX = new Node(Id.createFromLong(0x123FFFFFL, 32), "6");
    private static final Node NODE_LONG_PREFIX = new Node(Id.createFromLong(0x1234F000L, 32), "6");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private Bucket fixture = new Bucket(BASE_ID, 16, 4); // bucket for prefix of 16 bits, bucket capacity of 4
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustInsertNodes() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        assertEquals(NODE_0000, fixture.get(0));
        assertEquals(NODE_0100, fixture.get(1));
        assertEquals(2, fixture.size());
    }

    @Test
    public void mustFailTouchIfTimeIsBeforeLastTime() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_0100);
    }

    @Test
    public void mustFailTouchIfPrefixTooShort() {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_SHORT_PREFIX);
    }

    @Test
    public void mustNotFailTouchIfPrefixTooLong() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(0L), NODE_LONG_PREFIX);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
    }

    @Test
    public void mustFailToInsertNodesIfFull() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        assertEquals(Bucket.TouchResult.FULL, touchRes);
        
        assertEquals(NODE_0000, fixture.get(0));
        assertEquals(NODE_0100, fixture.get(1));
        assertEquals(NODE_1000, fixture.get(2));
        assertEquals(NODE_1100, fixture.get(3));
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustUpdateNode() {
        Bucket.TouchResult touchRes;
        
        assertEquals(0, fixture.size());
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111); // must fail, bucket is full
        assertEquals(Bucket.TouchResult.FULL, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0000); // keep time at 4L, even though previous was 5L -- it wasn't successful
        assertEquals(Bucket.TouchResult.UPDATED, touchRes);

        assertEquals(NODE_0100, fixture.get(0));
        assertEquals(NODE_1000, fixture.get(1));
        assertEquals(NODE_1100, fixture.get(2));
        assertEquals(NODE_0000, fixture.get(3));
        assertEquals(4, fixture.size());
    }
    
    @Test
    public void mustSplitInTo2Buckets() {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        
        Bucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(NODE_0000, buckets[0].get(0));
        assertEquals(NODE_0100, buckets[0].get(1));
        assertEquals(NODE_1000, buckets[1].get(0));
        assertEquals(NODE_1100, buckets[1].get(1));
        
        assertEquals(buckets[0].size(), 2);
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(3L));
        assertEquals(buckets[1].size(), 2);
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }

    @Test
    public void mustSplitInTo2BucketsWhere1BucketIsEmpty() {
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1111);
        
        Bucket[] buckets = fixture.split(1);
        
        assertEquals(2, buckets.length);
        
        assertEquals(NODE_1000, buckets[1].get(0));
        assertEquals(NODE_1100, buckets[1].get(1));
        assertEquals(NODE_1111, buckets[1].get(2));
        
        assertEquals(buckets[0].size(), 0);
        assertEquals(buckets[0].getLastUpdateTime(), Instant.MIN);
        assertEquals(buckets[1].size(), 3);
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }

    @Test
    public void mustSplitInTo4Buckets() {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        
        Bucket[] buckets = fixture.split(2);
        
        assertEquals(4, buckets.length);
        
        assertEquals(NODE_0000, buckets[0].get(0));
        assertEquals(NODE_0100, buckets[1].get(0));
        assertEquals(NODE_1000, buckets[2].get(0));
        assertEquals(NODE_1100, buckets[3].get(0));
        
        assertEquals(buckets[0].size(), 1);
        assertEquals(buckets[0].getLastUpdateTime(), BASE_TIME.plusMillis(1L));
        assertEquals(buckets[1].size(), 1);
        assertEquals(buckets[1].getLastUpdateTime(), BASE_TIME.plusMillis(3L));
        assertEquals(buckets[2].size(), 1);
        assertEquals(buckets[2].getLastUpdateTime(), BASE_TIME.plusMillis(2L));
        assertEquals(buckets[3].size(), 1);
        assertEquals(buckets[3].getLastUpdateTime(), BASE_TIME.plusMillis(4L));
    }
}
