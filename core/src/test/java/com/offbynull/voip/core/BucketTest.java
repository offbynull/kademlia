package com.offbynull.voip.core;

import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BucketTest {
    
    
    private static final Id BASE_ID = Id.createFromInteger(0x1234FFFFL, 32);
    
    private static final Node NODE_1 = new Node(Id.createFromInteger(0x12340700L, 32), "1");
    private static final Node NODE_2 = new Node(Id.createFromInteger(0x12340F00L, 32), "2");
    private static final Node NODE_3 = new Node(Id.createFromInteger(0x12341F00L, 32), "3");
    private static final Node NODE_4 = new Node(Id.createFromInteger(0x12343F00L, 32), "4");
    private static final Node NODE_5 = new Node(Id.createFromInteger(0x12347F00L, 32), "5");
    
    private static final Node NODE_SHORT_PREFIX = new Node(Id.createFromInteger(0x123FFFFFL, 32), "6");
    private static final Node NODE_LONG_PREFIX = new Node(Id.createFromInteger(0x1234F000L, 32), "6");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private Bucket fixture = new Bucket(BASE_ID, 16, 4); // bucket for prefix of 16 bits, bucket capacity of 4
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustInsertNodes() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_2);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        assertEquals(NODE_1, fixture.get(0));
        assertEquals(NODE_2, fixture.get(1));
        assertEquals(2, fixture.size());
    }

    @Test
    public void mustFailTouchIfTimeIsBeforeLastTime() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_2);
    }

    @Test
    public void mustFailTouchIfPrefixTooShort() {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_SHORT_PREFIX);
    }

    @Test
    public void mustFailTouchIfPrefixTooLong() {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_LONG_PREFIX);
    }

    @Test
    public void mustFailToInsertNodesIfFull() {
        Bucket.TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_2);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_3);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_4);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_5);
        assertEquals(Bucket.TouchResult.FULL, touchRes);
        
        assertEquals(NODE_1, fixture.get(0));
        assertEquals(NODE_2, fixture.get(1));
        assertEquals(NODE_3, fixture.get(2));
        assertEquals(NODE_4, fixture.get(3));
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustUpdateNode() {
        Bucket.TouchResult touchRes;
        
        assertEquals(0, fixture.size());
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_2);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_3);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_4);
        assertEquals(Bucket.TouchResult.INSERTED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_5); // must fail, bucket is full
        assertEquals(Bucket.TouchResult.FULL, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1); // keep time at 4L, even though previous was 5L -- it wasn't successful
        assertEquals(Bucket.TouchResult.UPDATED, touchRes);

        assertEquals(NODE_2, fixture.get(0));
        assertEquals(NODE_3, fixture.get(1));
        assertEquals(NODE_4, fixture.get(2));
        assertEquals(NODE_1, fixture.get(3));
        assertEquals(4, fixture.size());
    }
    
}
