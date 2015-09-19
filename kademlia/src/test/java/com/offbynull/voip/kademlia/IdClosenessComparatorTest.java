package com.offbynull.voip.kademlia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private static final Node NODE_1000 = new Node(Id.createFromLong(0x08L, 4), "8");
    private static final Node NODE_1001 = new Node(Id.createFromLong(0x09L, 4), "9");
    private static final Node NODE_1010 = new Node(Id.createFromLong(0x0AL, 4), "A");
    private static final Node NODE_1011 = new Node(Id.createFromLong(0x0BL, 4), "B");
    private static final Node NODE_1100 = new Node(Id.createFromLong(0x0CL, 4), "C");
    private static final Node NODE_1101 = new Node(Id.createFromLong(0x0DL, 4), "D");
    private static final Node NODE_1110 = new Node(Id.createFromLong(0x0EL, 4), "E");
    private static final Node NODE_1111 = new Node(Id.createFromLong(0x0FL, 4), "F");
    
    private IdClosenessComparator fixture = new IdClosenessComparator(NODE_0000.getId()); // 0000
    
    @Test
    public void mustIdentifyWhenEqual() {
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
    public void mustIdentifyLesserWhenPrefixesAreEqual() {
        Id o1 = NODE_1100.getId(); // 1100
        Id o2 = NODE_1110.getId(); // 1110
        
        int res1 = fixture.compare(o1, o2);
        assertEquals(-1, res1);
        int res2 = fixture.compare(o2, o1);
        assertEquals(1, res2);
    }

    @Test
    public void mustProperlySortLargeIdSpace() {
        List<Id> list = new ArrayList<>();
        
        // 1100 repeated multiple times
        Id baseId = Id.create(BitString.createFromString(
                "110011001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100"
                + "11001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100110011001100"
                + "1100110011001100110011001100"));
        
        fixture = new IdClosenessComparator(baseId);
        
        list.add(baseId.setBitsAsLong(0x00L, 0, 4));// first 4 bits turned to 0000
        list.add(baseId.setBitsAsLong(0x04L, 0, 4));// first 4 bits turned to 0100
        list.add(baseId.setBitsAsLong(0x08L, 0, 4));// first 4 bits turned to 1000
        list.add(baseId.setBitsAsLong(0x0CL, 0, 4));// first 4 bits turned to 1100 (nochange)
        list.add(baseId.setBitsAsLong(0x0EL, 0, 4));// first 4 bits turned to 1110
        list.add(baseId.setBitsAsLong(0x0FL, 0, 4));// first 4 bits turned to 1111
        
        Collections.sort(list, fixture);
        
        assertEquals(0x0CL, list.get(0).getBitsAsLong(0, 4));
        assertEquals(0x0EL, list.get(1).getBitsAsLong(0, 4));
        assertEquals(0x0FL, list.get(2).getBitsAsLong(0, 4));
        assertEquals(0x08L, list.get(3).getBitsAsLong(0, 4));
        assertEquals(0x04L, list.get(4).getBitsAsLong(0, 4));
        assertEquals(0x00L, list.get(5).getBitsAsLong(0, 4));
        
        list.forEach(System.out::println);
    }
    
}
