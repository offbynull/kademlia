package com.offbynull.voip.kademlia.model;

import static com.offbynull.voip.kademlia.model.TestUtils.verifyActivityChangeSetAdded;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyActivityChangeSetCounts;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyActivityChangeSetRemoved;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodesInActivities;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyPrefixMatches;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public final class RouteTreeTest {
    
    private static final Node NODE_0000 = new Node(Id.createFromLong(0x00L, 4), "0"); // 0000
    private static final Node NODE_0001 = new Node(Id.createFromLong(0x01L, 4), "1");
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x02L, 4), "2");
    private static final Node NODE_0011 = new Node(Id.createFromLong(0x03L, 4), "3");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x04L, 4), "4");
    private static final Node NODE_0101 = new Node(Id.createFromLong(0x05L, 4), "5");
    private static final Node NODE_0110 = new Node(Id.createFromLong(0x06L, 4), "6");
    private static final Node NODE_0111 = new Node(Id.createFromLong(0x07L, 4), "7");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x08L, 4), "8");
    private static final Node NODE_1001 = new Node(Id.createFromLong(0x09L, 4), "9");
    private static final Node NODE_1010 = new Node(Id.createFromLong(0x0AL, 4), "A");
    private static final Node NODE_1011 = new Node(Id.createFromLong(0x0BL, 4), "B");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x0CL, 4), "C");
    private static final Node NODE_1101 = new Node(Id.createFromLong(0x0DL, 4), "D");
    private static final Node NODE_1110 = new Node(Id.createFromLong(0x0EL, 4), "E");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x0FL, 4), "F");
    
    private static final Instant BASE_TIME = Instant.ofEpochMilli(0L);
    
    private RouteTree fixture;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    public RouteTreeTest() {
        SimpleRouteTreeSpecificationSupplier specSupplier = new SimpleRouteTreeSpecificationSupplier(NODE_0000.getId(), 2, 2, 2);
        fixture = new RouteTree(NODE_0000.getId(), specSupplier, specSupplier);        
    }

    @Test
    public void mustAddNodesToProperBuckets() throws Throwable {
        RouteTreeChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        verifyPrefixMatches(res.getKBucketPrefix(), "0001");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0001);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        verifyPrefixMatches(res.getKBucketPrefix(), "001");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0010);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        verifyPrefixMatches(res.getKBucketPrefix(), "001");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0011);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_0101);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_0110);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_0110);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_0111);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_0111);
    }

    @Test
    public void mustProperlyReplaceStaleNodesWithCacheIfAvailable() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0101);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_0110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_0111);
        
        RouteTreeChangeSet res;
        res = fixture.stale(NODE_0100);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0111);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_0111);
        
        res = fixture.stale(NODE_0101);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0110);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_0110);
    }

    @Test
    public void mustProperlyReplaceStaleNodesWithCacheWhenAvailable() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0101);
        
        RouteTreeChangeSet res;
        res = fixture.stale(NODE_0100);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.stale(NODE_0101);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_0110);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0110);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0100);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_0111);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 1, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0111);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getBucketChangeSet(), NODE_0101);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
    }

    @Test
    public void mustReplaceCacheNodesInBucket() throws Throwable {
        RouteTreeChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_1001);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_1001);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_1010);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getBucketChangeSet(), NODE_1010);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 0, 0, 0);
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_1011);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1011);
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_1100);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 0, 0);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1100);
        
        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_1101);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1011);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1101);
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_1110);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1100);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1110);
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_1111);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getBucketChangeSet(), 0, 0, 0);
        verifyActivityChangeSetCounts(res.getKBucketChangeSet().getCacheChangeSet(), 1, 1, 0);
        verifyActivityChangeSetRemoved(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1101);
        verifyActivityChangeSetAdded(res.getKBucketChangeSet().getCacheChangeSet(), NODE_1111);
        
        
        verifyNodesInActivities(fixture.dumpBucket(BitString.createFromString("1")), NODE_1001, NODE_1010);
    }

    @Test
    public void mustFindClosestSingleNode() throws Throwable {
        // all of the following nodes should be inserted in to buckets of kbuckets, not caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        Node res;
        
        res = fixture.find(NODE_1000.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1100);
        res = fixture.find(NODE_1001.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1100);
        res = fixture.find(NODE_1010.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1110); // this is correct ... remember bits are flipped if common prefixes are the same
        res = fixture.find(NODE_1100.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1100);
        res = fixture.find(NODE_1101.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1100);
        res = fixture.find(NODE_1110.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1110);
        res = fixture.find(NODE_1111.getId(), 1, false).get(0).getNode();
        assertEquals(res, NODE_1110);
    }

    @Test
    public void mustFindClosestNodesFirstExhaustive() throws Throwable {
        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        List<Activity> res;
        
        // NOTE THAT THIS TEST SHOWS SOMETHING REALLY IMPORTANT... it shows that the routetree will return the closest nodes (as defined by
        // the XOR metric) first, not just any node out of the buckets that the search hits.
        
        
        // for all of these, res is correct ... remember the XOR metric... comments are provided after each check showing the calculations
        // are correct
        //
        // also keep in mind that routetree will never return self and will never let you search for self
        res = fixture.find(NODE_0001.getId(), 7, true);
        verifyNodesInActivities(res, NODE_0001, NODE_0011, NODE_0010, NODE_0100, NODE_0111, NODE_1100, NODE_1110); 
        //                       XOR      0001       0001       0001       0001       0001       0001       0001
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0000       0010       0011       0101       0110       1101       1110
        //       (result in decimal)         0          2          3          5          6         13         14
        res = fixture.find(NODE_0010.getId(), 7, true);
        verifyNodesInActivities(res, NODE_0010, NODE_0011, NODE_0001, NODE_0111, NODE_0100, NODE_1110, NODE_1100); 
        //                       XOR      0010       0010       0010       0010       0010       0010       0010
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0000       0001       0011       0101       0110       1100       1110
        //       (result in decimal)         0          1          3          5          6         12         14
        res = fixture.find(NODE_0011.getId(), 7, true);
        verifyNodesInActivities(res, NODE_0011, NODE_0010, NODE_0001, NODE_0111, NODE_0100, NODE_1110, NODE_1100); 
        //                       XOR      0011       0011       0011       0011       0011       0011       0011
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0000       0001       0010       0100       0111       1101       1111
        //       (result in decimal)         0          1          2          4          7         13         15
        res = fixture.find(NODE_0100.getId(), 7, true);
        verifyNodesInActivities(res, NODE_0100, NODE_0111, NODE_0001, NODE_0010, NODE_0011, NODE_1100, NODE_1110); 
        //                       XOR      0100       0100       0100       0100       0100       0100       0100
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0000       0011       0101       0110       0111       1000       1010
        //       (result in decimal)         0          3          5          6          7          8         10
        res = fixture.find(NODE_0111.getId(), 7, true);
        verifyNodesInActivities(res, NODE_0111, NODE_0100, NODE_0011, NODE_0010, NODE_0001, NODE_1110, NODE_1100); 
        //                       XOR      0111       0111       0111       0111       0111       0111       0111
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0000       0011       0100       0101       0110       1001       1011
        //       (result in decimal)         0          3          4          5          6          9         11
        res = fixture.find(NODE_1000.getId(), 7, true);
        verifyNodesInActivities(res, NODE_1100, NODE_1110, NODE_0001, NODE_0010, NODE_0011, NODE_0100, NODE_0111); 
        //                       XOR      1000       1000       1000       1000       1000       1000       1000
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0100       0110       1001       1010       1011       1100       1111
        //       (result in decimal)         4          6          9         10         11         12         15
        res = fixture.find(NODE_1111.getId(), 7, true);
        verifyNodesInActivities(res, NODE_1110, NODE_1100, NODE_0111, NODE_0100, NODE_0011, NODE_0010, NODE_0001); 
        //                       XOR      1111       1111       1111       1111       1111       1111       1111
        //                                ----       ----       ----       ----       ----       ----       ----
        //                                0001       0011       1000       1011       1100       1101       1110
        //       (result in decimal)         1          3          8         11         12         13         14
    }
    
    @Test
    public void mustFindClosestNodesFirst() throws Throwable {
        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        List<Activity> res;
        
        // NOTE THAT THIS TEST SHOWS SOMETHING REALLY IMPORTANT... it shows that the routetree will return the closest nodes (as defined by
        // the XOR metric) first, not just any node out of the buckets that the search hits.
        
        
        // for all of these, res is correct ... remember the XOR metric... comments are provided after each check showing the calculations
        // are correct
        //
        // also keep in mind that routetree will never return self and will never let you search for self
        res = fixture.find(NODE_0001.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0001, NODE_0011, NODE_0010); 
        //                       XOR      0001       0001       0001
        //                                ----       ----       ----
        //                                0000       0010       0011
        //       (result in decimal)         0          2          3
        res = fixture.find(NODE_0010.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0010, NODE_0011, NODE_0001); 
        //                       XOR      0010       0010       0010
        //                                ----       ----       ----
        //                                0000       0001       0011
        //       (result in decimal)         0          1          3
        res = fixture.find(NODE_0011.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0011, NODE_0010, NODE_0001); 
        //                       XOR      0011       0011       0011
        //                                ----       ----       ----
        //                                0000       0001       0010
        //       (result in decimal)         0          1          2
        res = fixture.find(NODE_0100.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0100, NODE_0111, NODE_0001); 
        //                       XOR      0100       0100       0100
        //                                ----       ----       ----
        //                                0000       0011       0101
        //       (result in decimal)         0          3          5
        res = fixture.find(NODE_0111.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0111, NODE_0100, NODE_0011); 
        //                       XOR      0111       0111       0111
        //                                ----       ----       ----
        //                                0000       0011       0100
        //       (result in decimal)         0          3          4
        res = fixture.find(NODE_1000.getId(), 3, true);
        verifyNodesInActivities(res, NODE_1100, NODE_1110, NODE_0001); 
        //                       XOR      1000       1000       1000
        //                                ----       ----       ----
        //                                0100       0110       1001
        //       (result in decimal)         4          6          9
        res = fixture.find(NODE_1111.getId(), 3, true);
        verifyNodesInActivities(res, NODE_1110, NODE_1100, NODE_0111); 
        //                       XOR      1111       1111       1111
        //                                ----       ----       ----
        //                                0001       0011       1000
        //       (result in decimal)         1          3          8
    }

    // Disable for now, lock/unlock not used
//    @Test
//    public void mustFindClosestNodesThatAreNotLocked() throws Throwable {
//        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
//        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
//        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
//        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
//        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
//        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
//        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
//        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);
//
//        List<Activity> res;
//        
//        fixture.lock(NODE_0001);
//        fixture.lock(NODE_0010);
//        
//        res = fixture.find(NODE_0001.getId(), 3, true);
//        verifyNodesInActivities(res, NODE_0011, NODE_0100, NODE_0111); 
//    }

    @Test
    public void mustFindClosestNodesIncludingStale() throws Throwable {
        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        List<Activity> res;
        
        fixture.stale(NODE_0001);
        fixture.stale(NODE_0010);
        
        res = fixture.find(NODE_0001.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0001, NODE_0011, NODE_0010); 
    }

    @Test
    public void mustFindClosestNodesNotIncludingStale() throws Throwable {
        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        List<Activity> res;
        
        fixture.stale(NODE_0001);
        fixture.stale(NODE_0010);
        
        res = fixture.find(NODE_0001.getId(), 3, false);
        verifyNodesInActivities(res, NODE_0011, NODE_0100, NODE_0111); 
    }

    @Test
    public void mustFindClosestNodesFirstInTreeWithLargerBranching() throws Throwable {
        // recreate fixture to have 4 branches per node instead of 2, then do the same test as mustFindClosestNodesFirst
        SimpleRouteTreeSpecificationSupplier specSupplier = new SimpleRouteTreeSpecificationSupplier(NODE_0000.getId(), 4, 2, 2);
        fixture = new RouteTree(NODE_0000.getId(), specSupplier, specSupplier);        
        
        // all of the following nodes should be inserted in to buckets of kbuckets, they won't overflow in to the caches of kbuckets
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1110);

        List<Activity> res;
        
        // NOTE THAT THIS TEST SHOWS SOMETHING REALLY IMPORTANT... it shows that the routetree will return the closest nodes (as defined by
        // the XOR metric) first, not just any node out of the buckets that the search hits.
        
        
        // for all of these, res is correct ... remember the XOR metric... comments are provided after each check showing the calculations
        // are correct
        //
        // also keep in mind that routetree will never return self and will never let you search for self
        res = fixture.find(NODE_0001.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0001, NODE_0011, NODE_0010); 
        //                       XOR      0001       0001       0001
        //                                ----       ----       ----
        //                                0000       0010       0011
        //       (result in decimal)         0          2          3
        res = fixture.find(NODE_0010.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0010, NODE_0011, NODE_0001); 
        //                       XOR      0010       0010       0010
        //                                ----       ----       ----
        //                                0000       0001       0011
        //       (result in decimal)         0          1          3
        res = fixture.find(NODE_0011.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0011, NODE_0010, NODE_0001); 
        //                       XOR      0011       0011       0011
        //                                ----       ----       ----
        //                                0000       0001       0010
        //       (result in decimal)         0          1          2
        res = fixture.find(NODE_0100.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0100, NODE_0111, NODE_0001); 
        //                       XOR      0100       0100       0100
        //                                ----       ----       ----
        //                                0000       0011       0101
        //       (result in decimal)         0          3          5
        res = fixture.find(NODE_0111.getId(), 3, true);
        verifyNodesInActivities(res, NODE_0111, NODE_0100, NODE_0011); 
        //                       XOR      0111       0111       0111
        //                                ----       ----       ----
        //                                0000       0011       0100
        //       (result in decimal)         0          3          4
        res = fixture.find(NODE_1000.getId(), 3, true);
        verifyNodesInActivities(res, NODE_1100, NODE_1110, NODE_0001); 
        //                       XOR      1000       1000       1000
        //                                ----       ----       ----
        //                                0100       0110       1001
        //       (result in decimal)         4          6          9
        res = fixture.find(NODE_1111.getId(), 3, true);
        verifyNodesInActivities(res, NODE_1110, NODE_1100, NODE_0111); 
        //                       XOR      1111       1111       1111
        //                                ----       ----       ----
        //                                0001       0011       1000
        //       (result in decimal)         1          3          8
    }

    @Test
    public void mustReturnStagnantBucketsInOrder() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1110);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_0111);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_0011);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_0100);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_0001);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_0010);
        fixture.touch(BASE_TIME.plusMillis(8L), NODE_1101);
        fixture.touch(BASE_TIME.plusMillis(9L), NODE_1111);

        
        List<BitString> bitStringsOlderThan5L = fixture.getStagnantBuckets(BASE_TIME.plusMillis(7L));
        assertEquals(
                Arrays.asList(
                        BitString.createFromString("1"),
                        BitString.createFromString("01"),
                        BitString.createFromString("0001"),
                        BitString.createFromString("001")), 
                bitStringsOlderThan5L);
    }
    
    @Test
    public void mustNotRejectIfFindingSelfId() throws Throwable {
        fixture.find(NODE_0000.getId(), 1, true);
    }

    @Test
    public void mustRejectIfTouchingSelfId() throws Throwable {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_0000);
    }

    @Test
    public void mustRejectIfStalingSelfId() throws Throwable {
        expectedException.expect(IllegalArgumentException.class);
        fixture.stale(NODE_0000);
    }
}
