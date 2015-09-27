package com.offbynull.voip.kademlia.model;

import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodeChangeSetAdded;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodeChangeSetCounts;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodeChangeSetRemoved;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodeChangeSetUpdated;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodes;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public final class NearBucketTest {

    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0");
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private final NearBucket fixture = new NearBucket(NODE_100.getId(), 2);
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustRetainClosestNodesOnTouch() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_001, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_011, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_010, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_010);
        verifyNodeChangeSetRemoved(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);

        
        res = fixture.touch(NODE_101, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getAfterBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_111, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getAfterBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_110, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getAfterBucketChangeSet(), NODE_110);
        verifyNodeChangeSetRemoved(res.getAfterBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
    }

    @Test
    public void mustBeAbleToTouchSameNodeMultipleTimes() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_001, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_001, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 1);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetUpdated(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBeforeBucket(), NODE_001);
        verifyNodes(fixture.dumpAfterBucket());
    }

    @Test
    public void mustBeAbleToTouchSameCacheNodeMultipleTimes() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_001, true);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_001);
        
        res = fixture.touch(NODE_001, true);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 1);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetUpdated(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 1);
        verifyNodeChangeSetUpdated(res.getCacheChangeSet(), NODE_001);
        
        verifyNodes(fixture.dumpBeforeBucket(), NODE_001);
        verifyNodes(fixture.dumpAfterBucket());
    }

    @Test
    public void mustCacheNodesAndUseThemAsReplacements() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_000, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_000);
        
        res = fixture.touch(NODE_001, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_001);
        
        res = fixture.touch(NODE_010, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_010);
        
        res = fixture.touch(NODE_011, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_011);
        
        res = fixture.touch(NODE_101, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_101);
        
        res = fixture.touch(NODE_110, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_110);
        
        res = fixture.touch(NODE_111, true);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getCacheChangeSet(), NODE_111);
        
        verifyNodes(fixture.dumpAfterBucket(), NODE_101, NODE_110);
        verifyNodes(fixture.dumpBeforeBucket(), NODE_010, NODE_011);
    }

    @Test
    public void mustUseCacheNodesForBeforeBucketAndOverrideWhenTouchingWithNodeCloser() throws Throwable {
        NearBucketChangeSet res;

        fixture.touch(NODE_001, true);
        fixture.touch(NODE_010, true);

        res = fixture.touch(NODE_011, false);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetRemoved(res.getBeforeBucketChangeSet(), NODE_001);
        verifyNodeChangeSetAdded(res.getBeforeBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBeforeBucket(), NODE_010, NODE_011);
    }

    @Test
    public void mustUseCacheNodesForAfterBucketAndOverrideWhenTouchingWithNodeCloser() throws Throwable {
        NearBucketChangeSet res;

        fixture.touch(NODE_111, true);
        fixture.touch(NODE_110, true);

        res = fixture.touch(NODE_101, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetRemoved(res.getAfterBucketChangeSet(), NODE_111);
        verifyNodeChangeSetAdded(res.getAfterBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpAfterBucket(), NODE_101, NODE_110);
    }

    @Test
    public void mustUseCacheNodesForBeforeBucketAndNotOverrideWhenTouchingWithNodeFartherAway() throws Throwable {
        NearBucketChangeSet res;

        fixture.touch(NODE_010, true);
        fixture.touch(NODE_011, true);

        res = fixture.touch(NODE_001, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBeforeBucket(), NODE_010, NODE_011);
    }

    @Test
    public void mustUseCacheNodesForAfterBucketAndNotOverrideWhenTouchingWithNodeFartherAway() throws Throwable {
        NearBucketChangeSet res;

        fixture.touch(NODE_101, true);
        fixture.touch(NODE_110, true);

        res = fixture.touch(NODE_111, false);
        verifyNodeChangeSetCounts(res.getBeforeBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getAfterBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpAfterBucket(), NODE_101, NODE_110);
    }
//
//    @Test
//    public void mustRemoveNodeOutOfPeers() throws Throwable {
//        NearBucketChangeSet res;
//
//        fixture.touch(NODE_011, true);
//        fixture.touch(NODE_010, true);
//
//        fixture.touch(NODE_001, false);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
//        
//        
//        res = fixture.remove(NODE_011);
//        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
//        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 1, 0);
//        verifyNodeChangeSetRemoved(res.getCacheChangeSet(), NODE_011);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
//    }
//
//    @Test
//    public void mustRemoveNodeOutOfBothPeersAndBucket() throws Throwable {
//        NearBucketChangeSet res;
//
//        fixture.touch(NODE_011, true);
//        fixture.touch(NODE_010, true);
//
//        fixture.touch(NODE_001, false);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
//        
//        
//        res = fixture.remove(NODE_010);
//        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
//        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_010);
//        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
//        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 1, 0);
//        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_010);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_011);
//    }
//
//    @Test
//    public void mustFailToRemoveIfNodeDoesntExistInEitherPeersOrBucket() throws Throwable {
//        NearBucketChangeSet res;
//
//        fixture.touch(NODE_011, true);
//        fixture.touch(NODE_010, true);
//
//        fixture.touch(NODE_001, false);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
//        
//        res = fixture.remove(NODE_111);
//        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
//        
//        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
//    }
//    
//    @Test
//    public void mustPreventConflictingBucketNodeOnTouch() throws Throwable {
//        fixture.touch(NODE_001, false);
//        fixture.touch(NODE_010, false);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.touch(new Node(NODE_010.getId(), "fakelink"), false);
//    }
//
//    @Test
//    public void mustPreventConflictingPeerNodeOnTouchPeer() throws Throwable {
//        fixture.touch(NODE_001, true);
//        fixture.touch(NODE_010, true);
//        fixture.touch(NODE_110, true);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.touch(new Node(NODE_110.getId(), "fakelink"), true);
//    }
//    
//    @Test
//    public void mustPreventBucketNodeConflictingWithPeerNodeOnTouch() throws Throwable {
//        fixture.touch(NODE_001, false);
//        fixture.touch(NODE_010, false);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.touch(new Node(NODE_010.getId(), "fakelink"), true);
//    }
//
//    @Test
//    public void mustPreventPeerNodeConflictingWithBucketNodeOnTouch() throws Throwable {
//        fixture.touch(NODE_001, true);
//        fixture.touch(NODE_010, true);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.touch(new Node(NODE_010.getId(), "fakelink"), false);
//    }
//
//    @Test
//    public void mustPreventRemoveIfConflictingWithBucketNodeOnTouch() throws Throwable {
//        fixture.touch(NODE_001, false);
//        fixture.touch(NODE_010, false);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.remove(new Node(NODE_010.getId(), "fakelink"));
//    }
//
//    @Test
//    public void mustPreventRemoveIfConflictingWithPeerNodeOnTouch() throws Throwable {
//        fixture.touch(NODE_001, true);
//        fixture.touch(NODE_010, true);
//        
//        expectedException.expect(LinkMismatchException.class);
//        fixture.remove(new Node(NODE_010.getId(), "fakelink"));
//    }
}
