package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetRemoved;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodes;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class NodeNearSetTest {
    
    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private final NodeNearSet fixture = new NodeNearSet(NODE_000.getId(), 2);
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustRetainNodesWithTheLargestSharedPrefix() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_011);
        
        verifyNodes(fixture.dump(), NODE_011, NODE_111);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res, 1, 1, 0);
        verifyNodeChangeSetRemoved(res, NODE_111);
        verifyNodeChangeSetAdded(res, NODE_001);
        
        verifyNodes(fixture.dump(), NODE_001, NODE_011);
    }

    @Test
    public void mustNotShowChangeIfNodeIsTooFarAway() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);
        
        res = fixture.touch(NODE_010);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_010);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        verifyNodes(fixture.dump(), NODE_001, NODE_010);
    }

    // See "notion of closeness" section in notes for more information on how closeness is calculated
    @Test
    public void mustFlipBitsToIdentifyClosestNodeWhenSharedPrefixIsTheSame() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        res = fixture.touch(NODE_110);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_110);
        
        verifyNodes(fixture.dump(), NODE_110, NODE_111);
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        res = fixture.touch(NODE_100);
        verifyNodeChangeSetCounts(res, 1, 1, 0);
        verifyNodeChangeSetAdded(res, NODE_100);
        verifyNodeChangeSetRemoved(res, NODE_111);
        
        verifyNodes(fixture.dump(), NODE_100, NODE_110);
    }

    @Test
    public void mustRejectMultipleTouchesForSameIdButFromDifferentLinks() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        expectedException.expect(LinkConflictException.class);
        fixture.touch(new Node(NODE_111.getId(), "fakelink"));
    }
    
    @Test
    public void mustNotFailTouchIfTimeIsBeforeLastTime() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);
        
        res = fixture.touch(NODE_010);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_010);
    }
    
    @Test
    public void mustRetainClosestNodesWhenResizing() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_011);
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        assertEquals(2, fixture.size());
        assertEquals(2, fixture.getMaxSize());
        verifyNodes(fixture.dump(), NODE_011, NODE_111);
        
        res = fixture.resize(1);
        verifyNodeChangeSetCounts(res, 0, 1, 0);
        verifyNodeChangeSetRemoved(res, NODE_111);
        
        assertEquals(1, fixture.size());
        assertEquals(1, fixture.getMaxSize());
        verifyNodes(fixture.dump(), NODE_011);
    }

    @Test
    public void mustDumpNearestUpTo() throws Throwable {
        fixture.resize(10);
        
        fixture.touch(NODE_001);
        fixture.touch(NODE_010);
        fixture.touch(NODE_011);
        fixture.touch(NODE_100);
        fixture.touch(NODE_101);
        fixture.touch(NODE_110);
        fixture.touch(NODE_111);

        List<Node> res;
        res = fixture.dumpNearestBefore(NODE_011.getId(), 10);

        verifyNodes(res, NODE_001, NODE_010);
    }

    @Test
    public void mustDumpNearestUpToLimited() throws Throwable {
        fixture.resize(10);
        
        fixture.touch(NODE_001);
        fixture.touch(NODE_010);
        fixture.touch(NODE_011);
        fixture.touch(NODE_100);
        fixture.touch(NODE_101);
        fixture.touch(NODE_110);
        fixture.touch(NODE_111);

        List<Node> res;
        res = fixture.dumpNearestBefore(NODE_011.getId(), 1);

        verifyNodes(res, NODE_001);
    }

    @Test
    public void mustDumpNearestFrom() throws Throwable {
        fixture.resize(10);
        
        fixture.touch(NODE_001);
        fixture.touch(NODE_010);
        fixture.touch(NODE_011);
        fixture.touch(NODE_100);
        fixture.touch(NODE_101);
        fixture.touch(NODE_110);
        fixture.touch(NODE_111);

        List<Node> res;
        res = fixture.dumpNearestAfter(NODE_011.getId(), 10);

        verifyNodes(res, NODE_100, NODE_101, NODE_110, NODE_111);
    }

    @Test
    public void mustDumpNearestFromLimited() throws Throwable {
        fixture.resize(10);
        
        fixture.touch(NODE_001);
        fixture.touch(NODE_010);
        fixture.touch(NODE_011);
        fixture.touch(NODE_100);
        fixture.touch(NODE_101);
        fixture.touch(NODE_110);
        fixture.touch(NODE_111);

        List<Node> res;
        res = fixture.dumpNearestAfter(NODE_011.getId(), 2);

        verifyNodes(res, NODE_100, NODE_101);
    }
    
    @Test
    public void mustRemoveNodes() throws Throwable {
        fixture.touch(NODE_111);
        fixture.touch(NODE_011);
        assertEquals(2, fixture.size());
        
        NodeChangeSet res = fixture.remove(NODE_111);
        verifyNodeChangeSetCounts(res, 0, 1, 0);
        verifyNodeChangeSetRemoved(res, NODE_111);
        
        assertEquals(1, fixture.size());
        verifyNodes(fixture.dump(), NODE_011);
    }

    @Test
    public void mustRejectRemoveOfMissingNode() throws Throwable {
        fixture.touch(NODE_111);
        fixture.touch(NODE_011);
        assertEquals(2, fixture.size());
        
        NodeChangeSet res = fixture.remove(NODE_001);
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(2, fixture.size());
        verifyNodes(fixture.dump(), NODE_011, NODE_111);
    }

    @Test
    public void mustRejectRemovesForSameIdButFromDifferentLinks() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        expectedException.expect(LinkConflictException.class);
        fixture.remove(new Node(NODE_111.getId(), "fakelink"));
    }

    @Test
    public void mustRejectTouchesForSameIdButFromDifferentLinks() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_111);
        
        expectedException.expect(LinkConflictException.class);
        fixture.touch(new Node(NODE_111.getId(), "fakelink"));
    }
}
