package com.offbynull.voip.core;

import static com.offbynull.voip.core.TestUtils.verifyChangeSetAdded;
import static com.offbynull.voip.core.TestUtils.verifyChangeSetCounts;
import static com.offbynull.voip.core.TestUtils.verifyChangeSetRemoved;
import static com.offbynull.voip.core.TestUtils.verifyChangeSetUpdated;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;
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
    public void mustRetainNodesWithTheLargestSharedPrefix() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_011);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_011);
        
        assertEquals(Arrays.asList(NODE_111, NODE_011),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        verifyChangeSetCounts(res, 0, 0, 1);
        verifyChangeSetUpdated(res, NODE_011);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_001);
        verifyChangeSetCounts(res, 1, 1, 0);
        verifyChangeSetRemoved(res, NODE_111);
        verifyChangeSetAdded(res, NODE_001);
        
        assertEquals(Arrays.asList(NODE_011, NODE_001),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }

    // See "notion of closeness" section in notes for more information on how closeness is calculated
    @Test
    public void mustFlipBitsToIdentifyClosestNodeWhenSharedPrefixIsTheSame() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_110);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_110);
        
        assertEquals(Arrays.asList(NODE_111, NODE_110), 
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_111);
        verifyChangeSetCounts(res, 0, 0, 1);
        verifyChangeSetUpdated(res, NODE_111);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        verifyChangeSetCounts(res, 1, 1, 0);
        verifyChangeSetAdded(res, NODE_100);
        verifyChangeSetRemoved(res, NODE_111);
        
        assertEquals(Arrays.asList(NODE_110, NODE_100),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }

    @Test
    public void mustRejectMultipleTouchesForSameIdButFromDifferentLinks() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        expectedException.expect(EntryConflictException.class);
        fixture.touch(BASE_TIME.plusMillis(2L), new Node(NODE_111.getId(), "fakelink"));
    }
    
    @Test
    public void mustNotFailTouchIfTimeIsBeforeLastTime() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_001);
        
        res = fixture.touch(BASE_TIME.plusMillis(0L), NODE_010);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_010);
    }
    
    @Test
    public void mustRetainClosestNodesWhenResizing() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_011);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_011);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        assertEquals(2, fixture.size());
        assertEquals(2, fixture.getMaxSize());
        assertEquals(Arrays.asList(NODE_111, NODE_011),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
        
        res = fixture.resize(1);
        verifyChangeSetCounts(res, 0, 1, 0);
        verifyChangeSetRemoved(res, NODE_111);
        
        assertEquals(1, fixture.size());
        assertEquals(1, fixture.getMaxSize());
        assertEquals(Arrays.asList(NODE_011),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }
    
    @Test
    public void mustRemoveNodes() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_011);
        assertEquals(2, fixture.size());
        
        ChangeSet res = fixture.remove(NODE_111);
        verifyChangeSetCounts(res, 0, 1, 0);
        verifyChangeSetRemoved(res, NODE_111);
        
        assertEquals(1, fixture.size());
        assertEquals(Arrays.asList(NODE_011),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }

    @Test
    public void mustRejectRemoveOfMissingNode() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_011);
        assertEquals(2, fixture.size());
        
        ChangeSet res = fixture.remove(NODE_001);
        verifyChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(2, fixture.size());
        assertEquals(Arrays.asList(NODE_111, NODE_011),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }

    @Test
    public void mustRejectRemovesForSameIdButFromDifferentLinks() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        expectedException.expect(EntryConflictException.class);
        fixture.remove(new Node(NODE_111.getId(), "fakelink"));
    }

    @Test
    public void mustRejectTouchesForSameIdButFromDifferentLinks() throws Throwable {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        expectedException.expect(EntryConflictException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), new Node(NODE_111.getId(), "fakelink"));
    }
}
