package com.offbynull.voip.core;

import com.offbynull.voip.core.NearSet.ChangeSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

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
    
    @Test
    public void mustRetainNodesWithTheLargestSharedPrefix() {
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
    public void mustFlipBitsToIdentifyClosestNodeWhenSharedPrefixIsTheSame() {
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
    public void mustRejectMultipleTouchesForSameIdButFromDifferentLinks() {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), new Node(NODE_111.getId(), "fakelink"));
        verifyChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(Arrays.asList(NODE_111),
                fixture.dump().stream().map(x -> x.getNode()).collect(Collectors.toList()));
    }
    
    @Test
    public void mustNotFailTouchIfTimeIsBeforeLastTime() {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_001);
        
        res = fixture.touch(BASE_TIME.plusMillis(0L), NODE_010);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_010);
    }
    
    @Test
    public void mustRetainClosestNodesWhenResizing() {
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
    public void mustRemoveNodes() {
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
    public void mustRejectRemoveOfMissingNode() {
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
    public void mustRejectRemovesForSameIdButFromDifferentLinks() {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        res = fixture.remove(new Node(NODE_111.getId(), "fakelink"));
        verifyChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(NODE_111, fixture.dump().get(0).getNode());
    }

    @Test
    public void mustRejectTouchesForSameIdButFromDifferentLinks() {
        ChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_111);
        verifyChangeSetCounts(res, 1, 0, 0);
        verifyChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), new Node(NODE_111.getId(), "fakelink"));
        verifyChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(NODE_111, fixture.dump().get(0).getNode());
    }
    
    public void verifyChangeSetCounts(ChangeSet changeSet, int expectedAdded, int expectedRemoved, int expectedUpdated) {
        assertEquals(expectedAdded, changeSet.viewAdded().size());
        assertEquals(expectedRemoved, changeSet.viewRemoved().size());
        assertEquals(expectedUpdated, changeSet.viewUpdated().size());
    }

    public void verifyChangeSetAdded(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewAdded().size());
        
        Set<Node> actual = changeSet.viewAdded().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }

    public void verifyChangeSetRemoved(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        Set<Node> actual = changeSet.viewRemoved().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }

    public void verifyChangeSetUpdated(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewUpdated().size());
        
        Set<Node> actual = changeSet.viewUpdated().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }
}
