package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetRemoved;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodes;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public final class NearBucketTest {

    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private final NearBucket fixture = new NearBucket(NODE_000.getId(), 2);
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustRetainClosestNodesOnTouch() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustRetainClosestNodesOnTouchWhenInsertedBackwards() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustUsePeerNodesForBucket() throws Throwable {
        NearBucketChangeSet res;
        
        res = fixture.touchPeer(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_001);
        
        res = fixture.touchPeer(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_010);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
        
        res = fixture.touchPeer(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_011);
        
        res = fixture.touchPeer(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_100);
        
        res = fixture.touchPeer(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_101);
        
        res = fixture.touchPeer(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_110);
        
        res = fixture.touchPeer(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_111);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustUsePeerNodesForBucketWhenInsertedBackwards() throws Throwable {
        NearBucketChangeSet res;

        res = fixture.touchPeer(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_111);
        
        verifyNodes(fixture.dumpBucket(), NODE_111);
        
        res = fixture.touchPeer(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_110);
        
        verifyNodes(fixture.dumpBucket(), NODE_110, NODE_111);
        
        res = fixture.touchPeer(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_101);
        
        verifyNodes(fixture.dumpBucket(), NODE_101, NODE_110);
        
        res = fixture.touchPeer(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_100);
        
        verifyNodes(fixture.dumpBucket(), NODE_100, NODE_101);
        
        res = fixture.touchPeer(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_011);
        
        verifyNodes(fixture.dumpBucket(), NODE_011, NODE_100);
        
        res = fixture.touchPeer(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_010);
        
        verifyNodes(fixture.dumpBucket(), NODE_010, NODE_011);
        
        res = fixture.touchPeer(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_001);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustUsePeerNodesForBucketAndOverrideWhenTouchingWithNodeCloser() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);

        res = fixture.touch(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustUsePeerNodesForBucketAndNotOverrideWhenTouchingWithNodeFartherAway() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);

        res = fixture.touch(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_010, NODE_011);
    }

    @Test
    public void mustRemoveNodeOutOfBucket() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);

        fixture.touch(NODE_001);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
        
        
        res = fixture.remove(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_010, NODE_011);
    }

    @Test
    public void mustRemoveNodeOutOfPeers() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);

        fixture.touch(NODE_001);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
        
        
        res = fixture.remove(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 1, 0);
        verifyNodeChangeSetRemoved(res.getPeerChangeSet(), NODE_011);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }

    @Test
    public void mustRemoveNodeOutOfBothPeersAndBucket() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);

        fixture.touch(NODE_001);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
        
        
        res = fixture.remove(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 1, 0);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_010);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_011);
    }

    @Test
    public void mustProperlyInsertAfterIncreasingSize() throws Throwable {
        // insert in to bucket first, once bucket is full dump in to cache
        NearBucketChangeSet res;
        
        res = fixture.resize(3); // 2 to 3
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);

        res = fixture.touchPeer(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_111);
        
        verifyNodes(fixture.dumpBucket(), NODE_111);
        
        res = fixture.touchPeer(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_110);
        
        verifyNodes(fixture.dumpBucket(), NODE_110, NODE_111);
        
        res = fixture.touchPeer(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_101);
        
        verifyNodes(fixture.dumpBucket(), NODE_101, NODE_110, NODE_111);
        
        res = fixture.touchPeer(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_111);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_100);
        
        verifyNodes(fixture.dumpBucket(), NODE_100, NODE_101, NODE_110);
        
        res = fixture.touchPeer(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_011);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_110);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_011);
        
        verifyNodes(fixture.dumpBucket(), NODE_011, NODE_100, NODE_101);
        
        res = fixture.touchPeer(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_101);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_010);
        
        verifyNodes(fixture.dumpBucket(), NODE_010, NODE_011, NODE_100);
        
        res = fixture.touchPeer(NODE_001);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 1, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_001);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_100);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getPeerChangeSet(), NODE_001);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010, NODE_011);
    }

    @Test
    public void mustMoveFromPeerNodesToBucketNodesWhenIncreasingSize() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_111);
        fixture.touchPeer(NODE_110);
        fixture.touchPeer(NODE_101);
        fixture.touchPeer(NODE_100);
        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);
        fixture.touchPeer(NODE_001);
        
        
        res = fixture.resize(3); // 2 to 3
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010, NODE_011);
    }

    @Test
    public void mustTruncateBucketNodesWhenDecreasingSize() throws Throwable {
        NearBucketChangeSet res;

        fixture.touchPeer(NODE_111);
        fixture.touchPeer(NODE_110);
        fixture.touchPeer(NODE_101);
        fixture.touchPeer(NODE_100);
        fixture.touchPeer(NODE_011);
        fixture.touchPeer(NODE_010);
        fixture.touchPeer(NODE_001);
        
        res = fixture.resize(1); // 2 to 1
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 1, 0);
        verifyNodeChangeSetRemoved(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetCounts(res.getPeerChangeSet(), 0, 0, 0);
        verifyNodes(fixture.dumpBucket(), NODE_001);
    }
}
