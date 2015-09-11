package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetRemoved;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetUpdated;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodesInActivities;
import static com.offbynull.voip.kademlia.TestUtils.verifyTimeInActivities;
import java.time.Instant;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NodeActivitySetTest {

    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private final NodeActivitySet fixture = new NodeActivitySet(NODE_000.getId());
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustInsert() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME, NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);

        res = fixture.touch(BASE_TIME, NODE_010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_010);
    }

    @Test
    public void mustUpdate() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME, NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);
        
        res = fixture.touch(BASE_TIME, NODE_001);
        verifyActivityChangeSetCounts(res, 0, 0, 1);
        verifyActivityChangeSetUpdated(res, NODE_001);
    }

    @Test
    public void mustRemove() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME, NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);
        
        res = fixture.remove(NODE_001);
        verifyActivityChangeSetCounts(res, 0, 1, 0);
        verifyActivityChangeSetRemoved(res, NODE_001);
    }
    
    @Test
    public void mustGetNodes() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);

        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_010);
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_011);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_100);
        
        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_101);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_110);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_111);
        
        
        List<Activity> activities = fixture.getStagnantNodes(BASE_TIME.plusMillis(5L));
        verifyNodesInActivities(activities, NODE_001, NODE_010, NODE_011, NODE_100, NODE_101); 
        verifyTimeInActivities(activities,
                BASE_TIME.plusMillis(1L),
                BASE_TIME.plusMillis(2L),
                BASE_TIME.plusMillis(3L),
                BASE_TIME.plusMillis(4L),
                BASE_TIME.plusMillis(5L));
    }
    
    @Test
    public void mustPreventTouchingNodeIfLinkDifferent() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);
        
        expectedException.expect(LinkConflictException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), new Node(NODE_001.getId(), "fakelink"));
    }

    @Test
    public void mustPreventRemovingNodeIfLinkDifferent() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_001);
        
        expectedException.expect(LinkConflictException.class);
        fixture.remove(new Node(NODE_001.getId(), "fakelink"));
    }
    
}
