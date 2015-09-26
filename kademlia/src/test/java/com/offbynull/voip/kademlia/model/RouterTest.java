package com.offbynull.voip.kademlia.model;

import com.offbynull.voip.kademlia.model.Router;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import static com.offbynull.voip.kademlia.model.TestUtils.verifyNodes;
import java.time.Instant;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RouterTest {
    
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
    
    private Router fixture = new Router(NODE_0000.getId(), 2, 2, 2, 2);
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void mustRetainClosestNodesEvenIfNotInRoutingTable() throws Throwable {
        // touch all 1 buckets, from largest to smallest
        fixture.touch(BASE_TIME, NODE_1111); 
        fixture.touch(BASE_TIME, NODE_1110); // this node and the bucket befoer it will go in to the 1xxx bucket, the rest of these guys
                                             // will go in to the near set
        
        fixture.touch(BASE_TIME, NODE_1101);
        fixture.touch(BASE_TIME, NODE_1100);
        fixture.touch(BASE_TIME, NODE_1011);
        fixture.touch(BASE_TIME, NODE_1010);
        fixture.touch(BASE_TIME, NODE_1001);
        fixture.touch(BASE_TIME, NODE_1000);
        
        // get closest ids to NODE_1000, there should be 2 in the 1xxx bucket, and 2 in the nearset
        List<Node> ret = fixture.find(NODE_1000.getId(), 100); // try to get an excess amount of nodes to make sure we dont have > 4
        
        verifyNodes(ret, NODE_1000, NODE_1001, NODE_1110, NODE_1111);
    }

    @Test
    public void mustRetainClosestNodesEvenIfInRoutingTable() throws Throwable {
        // touch all 1 buckets, from smallest to largest
        fixture.touch(BASE_TIME, NODE_1000);
        fixture.touch(BASE_TIME, NODE_1001); // this node and the bucket befoer it will go in to the 1xxx bucket as well as the near set,
                                             // the rest of these guys will be discarded because the bucket is full and their farther away
                                             // than the top 2
        fixture.touch(BASE_TIME, NODE_1010);
        fixture.touch(BASE_TIME, NODE_1011);
        fixture.touch(BASE_TIME, NODE_1100);
        fixture.touch(BASE_TIME, NODE_1101);
        fixture.touch(BASE_TIME, NODE_1110);
        fixture.touch(BASE_TIME, NODE_1111);
        
        // get closest ids to NODE_1000, there should be 2 in the 1xxx bucket, and 2 in the nearset... but they're the same 2 so the
        // duplicates will go away
        List<Node> ret = fixture.find(NODE_1000.getId(), 100); // try to get an excess amount of nodes to make sure we dont have > 2
        
        verifyNodes(ret, NODE_1000, NODE_1001);
    }
    

    @Test
    public void mustRemoveStaleNodeFromNearBucketButNotFromRoutingTreeIfCacheEmpty() throws Throwable {
        // touch all 1 buckets, from smallest to largest
        fixture.touch(BASE_TIME, NODE_1000);
        fixture.touch(BASE_TIME, NODE_1001);
        
        fixture.stale(NODE_1000);
        
        // 1000 shouldn't be in the nearset anymore, but routingtree has reached desperation mode for 1xxx bucket because it has no cache
        // nodes to replace 1000 with so it keeps it and allows it to be accessed (until another 1xxx node comes along)
        List<Node> ret = fixture.find(NODE_1000.getId(), 100);

        verifyNodes(ret, NODE_1000, NODE_1001);
    }

    @Test
    public void mustRemoveStaleNodeFromNearBucketAndRoutingTreeIfCacheNotEmpty() throws Throwable {
        // touch all 1 buckets, from smallest to largest
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001); // this node and the bucket befoer it will go in to the 1xxx bucket as well as
                                                            // the near set, the rest of these guys will be discarded because the bucket is
                                                            // full and their farther away than the top 2
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1010);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1011);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_1100);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_1101); // cache for 1xxx bucket will contain this node and the one below it
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_1111); 
        
        fixture.stale(NODE_1000);
        
        List<Node> ret;
        
        // 1000 shouldn't be in the nearset or routingtree anymore. nearset should contain 1001 and 1111, while 1xxx bucket should also
        // contain 1001 and 1111
        //
        // remember how router works: in this case 1000 was removed, but 1111 came in its place (it was latest in the 1xxx cache) and in
        // doing so was added in to nearbucket. what about 1010? when 1010 was touched, it wasn't close enough to enter the nearbucket and
        // it didn't become part of the routingtree (because the 1xxx bucket was full -- being inserted in to the 1xxx bucket's replacement
        // cache doesn't count) so it wasn't added as a replacement/cache node for the nearbucket
        ret = fixture.find(NODE_1000.getId(), 100);
        verifyNodes(ret, NODE_1001, NODE_1111);
    }    
    
    @Test
    public void mustMakeSearchableAgainIfStaleNodeRetouched() throws Throwable {
        // touch all 1 buckets, from smallest to largest
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1010);
        
        fixture.stale(NODE_1000); // 1000 removed from nearset, and 1000 replaced with 1010 in kbucket
        
        List<Node> ret;
        
        ret = fixture.find(NODE_1000.getId(), 100);
        verifyNodes(ret, NODE_1001, NODE_1010);
        
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1000);
        
        ret = fixture.find(NODE_1000.getId(), 100);
        verifyNodes(ret, NODE_1000, NODE_1001, NODE_1010); // 1001 and 1010 in kbucket, and 1000 in nearset
    }

//    @Test
//    public void mustMakeSearchableAgainIfLockedNodeUnlocked() throws Throwable {
//        // touch all 1 buckets, from smallest to largest
//        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
//        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001);
//        fixture.touch(BASE_TIME.plusMillis(3L), NODE_1010);
//        fixture.touch(BASE_TIME.plusMillis(4L), NODE_1011);
//        
//        fixture.lock(NODE_1000); // 1000 removed from nearset, and 1000 locked in kbucket (NOT REMOVED/REPLACED)
//        
//        List<Node> ret;
//        
//        ret = fixture.find(NODE_1000.getId(), 100);
//        verifyNodes(ret, NODE_1001); // 1000 is not findable because locked and not in nearset, 1010+1011 in kbucket cache / not in nearset
//        
//        fixture.unlock(NODE_1000); // 1000 re-added to nearset, and 1000 unlocked in kbucket
//        
//        ret = fixture.find(NODE_1000.getId(), 100);
//        verifyNodes(ret, NODE_1000, NODE_1001); // 1000 and 1001 findable in kbucket, and exist in nearset
//    }
//
//    @Test
//    public void mustNotAllowLockIfStale() throws Throwable {
//        // touch all 1 buckets, from smallest to largest
//        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
//        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001);
//        
//        fixture.stale(NODE_1000);
//        
//        expectedException.expect(IllegalArgumentException.class);
//        fixture.lock(NODE_1000);
//    }
//
//    @Test
//    public void mustNotAllowUnlockIfStale() throws Throwable {
//        // touch all 1 buckets, from smallest to largest
//        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
//        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001);
//        
//        fixture.stale(NODE_1000);
//        
//        expectedException.expect(IllegalArgumentException.class);
//        fixture.unlock(NODE_1000);
//    }
//
//    @Test
//    public void mustNotAllowStaleIfLocked() throws Throwable {
//        // touch all 1 buckets, from smallest to largest
//        fixture.touch(BASE_TIME.plusMillis(1L), NODE_1000);
//        fixture.touch(BASE_TIME.plusMillis(2L), NODE_1001);
//        
//        fixture.lock(NODE_1000);
//        
//        expectedException.expect(IllegalArgumentException.class);
//        fixture.stale(NODE_1000);
//    }
}
