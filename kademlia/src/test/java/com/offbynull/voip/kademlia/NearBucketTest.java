package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetCounts;
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
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_010);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 1, 0, 0);
        verifyNodeChangeSetAdded(res.getBucketChangeSet(), NODE_010);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_011);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_100);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_101);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_110);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(NODE_111);
        verifyNodeChangeSetCounts(res.getBucketChangeSet(), 0, 0, 0);
        verifyNodeChangeSetCounts(res.getCacheChangeSet(), 0, 0, 0);
        
        verifyNodes(fixture.dumpBucket(), NODE_001, NODE_010);
    }
    
}
