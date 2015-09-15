package com.offbynull.voip.kademlia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class IdClosenessComparatorTest {

    private static final Node NODE_0000 = new Node(Id.createFromLong(0x00L, 4), "0"); // 0000
    private static final Node NODE_0001 = new Node(Id.createFromLong(0x01L, 4), "1");
    private static final Node NODE_0010 = new Node(Id.createFromLong(0x02L, 4), "2");
    private static final Node NODE_0011 = new Node(Id.createFromLong(0x03L, 4), "3");
    private static final Node NODE_0100 = new Node(Id.createFromLong(0x04L, 4), "4");
    private static final Node NODE_0101 = new Node(Id.createFromLong(0x05L, 4), "5");
    private static final Node NODE_0110 = new Node(Id.createFromLong(0x06L, 4), "6");
    private static final Node NODE_0111 = new Node(Id.createFromLong(0x07L, 4), "7");
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x08L, 4), "8"); // 0000
    private static final Node NODE_1001 = new Node(Id.createFromLong(0x09L, 4), "9");
    private static final Node NODE_1010 = new Node(Id.createFromLong(0x0AL, 4), "A");
    private static final Node NODE_1011 = new Node(Id.createFromLong(0x0BL, 4), "B");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x0CL, 4), "C");
    private static final Node NODE_1101 = new Node(Id.createFromLong(0x0DL, 4), "D");
    private static final Node NODE_1110 = new Node(Id.createFromLong(0x0EL, 4), "E");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x0FL, 4), "F");
    
    private IdClosenessComparator fixture = new IdClosenessComparator(NODE_0000.getId()); // 000
    
    @Test
    public void mustIdentifyWhenEqual() {
        Id o1 = Id.createFromLong(0x1L, 3); // 0001
        
        int res = fixture.compare(NODE_0001.getId(), NODE_0001.getId());
        assertEquals(0, res);
    }

    @Test
    public void mustIdentifyByLargerPrefix() {
        Id o1 = NODE_0001.getId(); // 0001
        Id o2 = NODE_0011.getId(); // 0011
        
        int res1 = fixture.compare(o1, o2);
        assertEquals(-1, res1);
        int res2 = fixture.compare(o2, o1);
        assertEquals(1, res2);
    }
    
    @Test
    public void mustFlipBitsToIdentifyWhenArgumentsHaveEqualPrefix() {
        Id o1 = NODE_1100.getId(); // 1100
        Id o2 = NODE_1110.getId(); // 1110
        
        int res1 = fixture.compare(o1, o2);
        assertEquals(-1, res1);
        int res2 = fixture.compare(o2, o1);
        assertEquals(1, res2);
    }

    @Test
    public void mustProperlySortWhenNoPrefixPresent() {
        List<Id> list = new ArrayList<>();
        
        fixture = new IdClosenessComparator(NODE_0000.getId());
        
        list.add(NODE_0000.getId());
        list.add(NODE_0001.getId());
        list.add(NODE_0010.getId());
        list.add(NODE_0011.getId());
        list.add(NODE_0100.getId());
        list.add(NODE_0101.getId());
        list.add(NODE_0110.getId());
        list.add(NODE_0111.getId());
        list.add(NODE_1000.getId());
        list.add(NODE_1001.getId());
        list.add(NODE_1010.getId());
        list.add(NODE_1011.getId());
        list.add(NODE_1100.getId());
        list.add(NODE_1101.getId());
        list.add(NODE_1110.getId());
        list.add(NODE_1111.getId());
        
        Collections.sort(list, fixture);
        
        list.forEach(System.out::println);
    }
    
}
