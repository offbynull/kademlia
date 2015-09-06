package com.offbynull.voip.core;

import com.offbynull.voip.core.NearSet.TouchResult;
import java.time.Instant;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class NearSetTest {
    
    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private final NearSet fixture = new NearSet(NODE_000.getId(), 2);
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustRetainNodesWithTheLargestSharedPrefix() {
        TouchResult res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        assertEquals(TouchResult.INSERTED, res);
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_011);
        assertEquals(TouchResult.INSERTED, res);
        
        assertEquals(Arrays.asList(NODE_111, NODE_011), fixture.dump());
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        assertEquals(TouchResult.UPDATED, res);
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_001);
        assertEquals(TouchResult.REPLACED, res);
        
        assertEquals(Arrays.asList(NODE_011, NODE_001), fixture.dump());
    }

    // See "notion of closeness" section in notes for more information on how closeness is calculated
    @Test
    public void mustProperlyFlipBitsToIdentifyClosestNodeWhenSharedPrefixIsTheSame() {
        TouchResult res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        assertEquals(TouchResult.INSERTED, res);
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_110);
        assertEquals(TouchResult.INSERTED, res);
        
        assertEquals(Arrays.asList(NODE_111, NODE_110), fixture.dump());
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_111);
        assertEquals(TouchResult.UPDATED, res);
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        assertEquals(TouchResult.REPLACED, res);
        
        assertEquals(Arrays.asList(NODE_110, NODE_100), fixture.dump());
    }

    @Test
    public void mustRejectMultipleTouchesForSameIdButFromDifferentLinks() {
        TouchResult res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        assertEquals(TouchResult.INSERTED, res);
        res = fixture.touch(BASE_TIME.plusMillis(2L), new Node(NODE_111.getId(), "fakelink"));
        assertEquals(TouchResult.IGNORED, res);
        
        assertEquals(Arrays.asList(NODE_111), fixture.dump());
    }
    
    @Test
    public void mustFailTouchIfTimeIsBeforeLastTime() {
        TouchResult touchRes;
        
        touchRes = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        assertEquals(TouchResult.INSERTED, touchRes);
        
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(0L), NODE_010);
    }
}
