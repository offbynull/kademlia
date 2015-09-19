package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetRemoved;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetUpdated;
import java.time.Instant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class NodeMostRecentSetTest {
    
    private static final Id BASE_ID = Id.createFromLong(0x12340000L, 32);
    
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x12342000L, 32), "1");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x12344000L, 32), "2");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x12348000L, 32), "3");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x1234C000L, 32), "4");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x1234F000L, 32), "5");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private NodeMostRecentSet fixture = new NodeMostRecentSet(BASE_ID, 4); // bucket for prefix of 16 bits, bucket capacity of 4
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustInsertNodes() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(2, fixture.size());
    }

    @Test
    public void mustInsertNodesWhenProvidedInBackwardsOrder() throws Throwable {
        ActivityChangeSet res;

        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
       
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotOverrideExistingNodesIfBucketFullAndTimestampUnchanged() throws Throwable {
        ActivityChangeSet res;

        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        verifyActivityChangeSetCounts(res, 0, 0, 0);    // should this be updated instead of ignored?
                                                     //  unsure... if all nodes being added have the same time, should the nodes added later
                                                     //  be seen as "more recent" than the ones previous? keep as-is for now.
       
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotOverrideExistingNodesIfBucketFullAndTimestampEarlier() throws Throwable {
        ActivityChangeSet res;

        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(0L), NODE_1111);
        verifyActivityChangeSetCounts(res, 0, 0, 0);
       
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustRejectNodeInsertionIfFullAndInPast() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(0L), NODE_1111);
        verifyActivityChangeSetCounts(res, 0, 0, 0);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustFailToRemoveNodeIfNotExists() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.remove(NODE_1100);
        verifyActivityChangeSetCounts(res, 0, 0, 0);
    }
    
    @Test
    public void mustRemoveNode() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);

        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(2, fixture.size());
        
        res = fixture.remove(NODE_0100);
        verifyActivityChangeSetCounts(res, 0, 1, 0);
        verifyActivityChangeSetRemoved(res, NODE_0100);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(1, fixture.size());
    }
    
    @Test
    public void mustAllowNodeInsertionIfNodeRemoved() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.remove(NODE_1100);
        verifyActivityChangeSetCounts(res, 0, 1, 0);
        verifyActivityChangeSetRemoved(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1111);
        
        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1111, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustUpdateNode() throws Throwable {
        ActivityChangeSet res;
        
        assertEquals(0, fixture.size());
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(0L), NODE_1111); // must fail, bucket is full and too far in past
        verifyActivityChangeSetCounts(res, 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0010);
        verifyActivityChangeSetCounts(res, 0, 0, 1);
        verifyActivityChangeSetUpdated(res, NODE_0010);

        assertEquals(NODE_0100, fixture.dump().get(0).getNode());
        assertEquals(NODE_1000, fixture.dump().get(1).getNode());
        assertEquals(NODE_0010, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustNotUpdateNodeIfFullButTimestampIsTheSame() throws Throwable {
        ActivityChangeSet res;
        
        assertEquals(0, fixture.size());
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        verifyActivityChangeSetCounts(res, 0, 0, 0);

        assertEquals(NODE_0010, fixture.dump().get(0).getNode());
        assertEquals(NODE_0100, fixture.dump().get(1).getNode());
        assertEquals(NODE_1000, fixture.dump().get(2).getNode());
        assertEquals(NODE_1100, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }
    
    @Test
    public void mustUpdateNodeWhenProvidedInBackwardsOrder() throws Throwable {
        ActivityChangeSet res;
        
        assertEquals(0, fixture.size());

        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1111);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1100);

        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1000);

        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_0100);
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        verifyActivityChangeSetCounts(res, 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0010);
        verifyActivityChangeSetCounts(res, 1, 1, 0);
        verifyActivityChangeSetAdded(res, NODE_0010);
        verifyActivityChangeSetRemoved(res, NODE_0100);

        assertEquals(NODE_1000, fixture.dump().get(0).getNode());
        assertEquals(NODE_0010, fixture.dump().get(1).getNode());
        assertEquals(NODE_1100, fixture.dump().get(2).getNode());
        assertEquals(NODE_1111, fixture.dump().get(3).getNode());
        assertEquals(4, fixture.size());
    }

    @Test
    public void mustRejectRemovesForSameIdButFromDifferentLinks() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1111);
        
        expectedException.expect(LinkMismatchException.class);
        fixture.remove(new Node(NODE_1111.getId(), "fakelink"));
    }
    

    @Test
    public void mustRejectTouchesForSameIdButFromDifferentLinks() throws Throwable {
        ActivityChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1111);
        verifyActivityChangeSetCounts(res, 1, 0, 0);
        verifyActivityChangeSetAdded(res, NODE_1111);
        
        expectedException.expect(LinkMismatchException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), new Node(NODE_1111.getId(), "fakelink"));
    }

    @Test
    public void mustGetLatestActivityTime() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_1111);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0010);

        assertEquals(BASE_TIME.plusMillis(5L), fixture.lastestActivityTime());
    }

    @Test
    public void mustGetNullForLatestActivityTimeIfEmpty() throws Throwable {
        assertNull(fixture.lastestActivityTime());
    }
}
