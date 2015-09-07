package com.offbynull.voip.core;

import com.offbynull.voip.core.LeastRecentlySeenSet.RemoveResult;
import com.offbynull.voip.core.LeastRecentlySeenSet.TouchResult;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LeastRecentlySeenSetTest {
    
    private static final Id BASE_ID = Id.createFromLong(0x12340000L, 32);
    
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x12342000L, 32), "1");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x12344000L, 32), "2");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x12348000L, 32), "3");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x1234C000L, 32), "4");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x1234F000L, 32), "5");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private LeastRecentlySeenSet fixture = new LeastRecentlySeenSet(BASE_ID, 4); // bucket for prefix of 16 bits, bucket capacity of 4
    

    @Test
    public void mustInsertNodes() {
        TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(2, fixture.size());
    }

    @Test
    public void mustInsertNodesWhenProvidedInBackwardsOrder() {
        TouchResult touchRes;

        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
       
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotOverrideExistingNodesIfBucketFullAndTimestampUnchanged() {
        TouchResult touchRes;

        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        assertEquals(TouchResult.IGNORED, touchRes);
       
        assertEquals(NODE_1100, fixture.dump().get(0).getNode());
        assertEquals(NODE_1000, fixture.dump().get(1).getNode());
        assertEquals(NODE_0100, fixture.dump().get(2).getNode());
        assertEquals(NODE_0010, fixture.dump().get(3).getNode());
        
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotOverrideExistingNodesIfBucketFullAndTimestampLater() {
        TouchResult touchRes;

        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_1111);
        assertEquals(TouchResult.IGNORED, touchRes);
       
        assertEquals(NODE_1100, fixture.dump().get(0).getNode());
        assertEquals(NODE_1000, fixture.dump().get(1).getNode());
        assertEquals(NODE_0100, fixture.dump().get(2).getNode());
        assertEquals(NODE_0010, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustRejectNodeInsertionIfFullAndInFuture() {
        TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        assertEquals(TouchResult.IGNORED, touchRes);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustFailToRemoveNodeIfNotExists() {
        RemoveResult removeRes;
        
        removeRes = fixture.remove(NODE_1100);
        assertEquals(RemoveResult.NOT_FOUND, removeRes);
    }
    
    @Test
    public void mustRemoveNode() {
        TouchResult touchRes;
        RemoveResult removeRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);

        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(2, fixture.size());
        
        removeRes = fixture.remove(NODE_0100);
        assertEquals(RemoveResult.REMOVED, removeRes);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(1, fixture.size());
    }
    
    @Test
    public void mustAllowNodeInsertionIfNodeRemoved() {
        TouchResult touchRes;
        RemoveResult removeRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        removeRes = fixture.remove(NODE_1100);
        assertEquals(RemoveResult.REMOVED, removeRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1111, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustUpdateNode() {
        TouchResult touchRes;
        
        assertEquals(0, fixture.size());
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111); // must fail, bucket is full and too far in future
        assertEquals(TouchResult.IGNORED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);

        assertEquals(NODE_0100, fixture.dump().get(0).getNode());
        assertEquals(NODE_1000, fixture.dump().get(1).getNode());
        assertEquals(NODE_1100, fixture.dump().get(2).getNode());
        assertEquals(NODE_0010, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotUpdateNodeIfFullButTimestampIsTheSame() {
        TouchResult touchRes;
        
        assertEquals(0, fixture.size());
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1111);
        assertEquals(TouchResult.IGNORED, touchRes);

        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustUpdateNodeWhenProvidedInBackwardsOrder() {
        TouchResult touchRes;
        
        assertEquals(0, fixture.size());

        touchRes = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        assertEquals(TouchResult.UPDATED, touchRes);

        touchRes = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0010);
        assertEquals(TouchResult.UPDATED, touchRes);

        assertEquals(NODE_0100, fixture.dump().get(0).getNode());
        assertEquals(NODE_1000, fixture.dump().get(1).getNode());
        assertEquals(NODE_1100, fixture.dump().get(2).getNode());
        assertEquals(NODE_0010, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }
    
    @Test
    public void mustRejectMultipleTouchesForSameIdButFromDifferentLinks() {
        TouchResult res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        assertEquals(TouchResult.UPDATED, res);
        res = fixture.touch(BASE_TIME.plusMillis(2L), new Node(NODE_1111.getId(), "fakelink"));
        assertEquals(TouchResult.CONFLICTED, res);
        
        assertEquals(NODE_1111, fixture.dump().get(0).getNode());
    }

    @Test
    public void mustRejectRemovesForSameIdButFromDifferentLinks() {
        TouchResult touchRes;
        RemoveResult removeRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        assertEquals(TouchResult.UPDATED, touchRes);
        removeRes = fixture.remove(new Node(NODE_1111.getId(), "fakelink"));
        assertEquals(RemoveResult.CONFLICTED, removeRes);
        
        assertEquals(NODE_1111, fixture.dump().get(0).getNode());
    }
}
