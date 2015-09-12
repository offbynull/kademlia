package com.offbynull.voip.kademlia;

import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;

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
    
    private RouteTree fixture = RouteTree.create(
            NODE_000.getId(),
            new SimpleRouteTreeSpecificationSupplier(NODE_000.getId(), 1, 2, 1));

    @Test
    public void must() throws Throwable {
        fixture.touch(BASE_TIME.plusMillis(1L), NODE_001);
        fixture.touch(BASE_TIME.plusMillis(2L), NODE_010);
        fixture.touch(BASE_TIME.plusMillis(3L), NODE_011);
        fixture.touch(BASE_TIME.plusMillis(4L), NODE_100);
        fixture.touch(BASE_TIME.plusMillis(5L), NODE_101);
        fixture.touch(BASE_TIME.plusMillis(6L), NODE_110);
        fixture.touch(BASE_TIME.plusMillis(7L), NODE_111);
    }
    
}
