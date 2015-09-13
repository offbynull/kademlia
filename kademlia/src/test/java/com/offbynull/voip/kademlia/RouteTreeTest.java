package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyActivityChangeSetRemoved;
import static com.offbynull.voip.kademlia.TestUtils.verifyPrefixMatches;
import java.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class RouteTreeTest {
    
    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private RouteTree fixture;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    public RouteTreeTest() {
        SimpleRouteTreeSpecificationSupplier specSupplier = new SimpleRouteTreeSpecificationSupplier(NODE_000.getId(), 2, 2, 2);
        fixture = new RouteTree(NODE_000.getId(), specSupplier, specSupplier);        
    }

    @Test
    public void mustAddNodesToProperBuckets() throws Throwable {
        RouteTreeChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyPrefixMatches(res.getKBucketPrefix(), "001");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_001);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_010);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_011);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_110);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_111);
    }

    @Test
    public void mustProperlyReplaceStaleNodesWithCacheIfAvailable() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
        
        RouteTreeChangeSet res;
        res = fixture.stale(NODE_100);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_111);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_111);
        
        res = fixture.stale(NODE_101);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_110);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_110);
    }

    @Test
    public void mustProperlyReplaceStaleNodesWithCacheWhenAvailable() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        
        RouteTreeChangeSet res;
        res = fixture.stale(NODE_100);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.stale(NODE_101);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_110);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_111);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
    }
    
    @Test
    public void mustRejectIfTouchingSelfId() throws Throwable {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_000);
    }

    @Test
    public void mustRejectIfStalingSelfId() throws Throwable {
        expectedException.expect(IllegalArgumentException.class);
        fixture.stale(NODE_000);
    }
}
