package com.offbynull.voip.kademlia;

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
    
    private RouteTree fixture = new RouteTree(
            NODE_000.getId(),
            new SimpleRouteTreeSpecificationSupplier(NODE_000.getId(), 1, 2, 2));

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustRejectIfTouchingSelfId() throws Throwable {
        expectedException.expect(IllegalArgumentException.class);
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_000);
    }

    @Test
    public void must() throws Throwable {
        RouteTreeChangeSet res;
        
        res = fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        verifyPrefixMatches(res.getKBucketPrefix(), "001");
        
        res = fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        verifyPrefixMatches(res.getKBucketPrefix(), "01");
        
        res = fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        verifyPrefixMatches(res.getKBucketPrefix(), "0");
        
        res = fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        
        res = fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        
        res = fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
        
        res = fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
        verifyPrefixMatches(res.getKBucketPrefix(), "1");
    }
    
}
