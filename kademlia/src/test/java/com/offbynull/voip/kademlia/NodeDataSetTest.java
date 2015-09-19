package com.offbynull.voip.kademlia;

import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetAdded;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetCounts;
import static com.offbynull.voip.kademlia.TestUtils.verifyNodeChangeSetRemoved;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class NodeDataSetTest {
    
    private static final Node NODE_000 = new Node(Id.createFromLong(0x00L, 3), "0"); // 000
    private static final Node NODE_001 = new Node(Id.createFromLong(0x01L, 3), "1");
    private static final Node NODE_010 = new Node(Id.createFromLong(0x02L, 3), "2");
    private static final Node NODE_011 = new Node(Id.createFromLong(0x03L, 3), "3");
    private static final Node NODE_100 = new Node(Id.createFromLong(0x04L, 3), "4");
    private static final Node NODE_101 = new Node(Id.createFromLong(0x05L, 3), "5");
    private static final Node NODE_110 = new Node(Id.createFromLong(0x06L, 3), "6");
    private static final Node NODE_111 = new Node(Id.createFromLong(0x07L, 3), "7");
    
    private final NodeDataSet fixture = new NodeDataSet(NODE_000.getId());
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Test
    public void mustInsert() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.put(NODE_001, "key1", "value1");
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);
        
        Object value = fixture.get(NODE_001, "key1");
        assertEquals("value1", value);
    }

    @Test
    public void mustUpdate() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.put(NODE_001, "key1", "value1");
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);

        res = fixture.put(NODE_001, "key1", "value2");
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        Object value = fixture.get(NODE_001, "key1");
        assertEquals("value2", value);
    }

    @Test
    public void mustGetAll() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.put(NODE_001, "key1", "value1");
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);

        res = fixture.put(NODE_001, "key2", "value2");
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        Map<Object, Object> value = fixture.getAll(NODE_001);
        assertEquals(2, value.size());
        assertEquals("value1", value.get("key1"));
        assertEquals("value2", value.get("key2"));
    }

    @Test
    public void mustRemove() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.put(NODE_001, "key1", "value1");
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);
        
        Object value = fixture.get(NODE_001, "key1");
        assertEquals("value1", value);

        res = fixture.remove(NODE_001, "key1");
        verifyNodeChangeSetCounts(res, 0, 1, 0);
        verifyNodeChangeSetRemoved(res, NODE_001);
        
        value = fixture.get(NODE_001, "key1");
        assertNull(value);
    }

    @Test
    public void mustFailRemoveWhenDoesNotExist() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.remove(NODE_001, "key1");
        verifyNodeChangeSetCounts(res, 0, 0, 0);
    }
    
    @Test
    public void mustRemoveAll() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.put(NODE_001, "key1", "value1");
        verifyNodeChangeSetCounts(res, 1, 0, 0);
        verifyNodeChangeSetAdded(res, NODE_001);

        res = fixture.put(NODE_001, "key2", "value2");
        verifyNodeChangeSetCounts(res, 0, 0, 0);
        
        res = fixture.removeAll(NODE_001);
        verifyNodeChangeSetCounts(res, 0, 1, 0);
        verifyNodeChangeSetRemoved(res, NODE_001);
        
        Object value;
        
        value = fixture.get(NODE_001, "key1");
        assertNull(value);
        
        value = fixture.get(NODE_001, "key2");
        assertNull(value);
    }

    @Test
    public void mustFailRemoveAllWhenDoesNotExist() throws Throwable {
        NodeChangeSet res;
        
        res = fixture.removeAll(NODE_001);
        verifyNodeChangeSetCounts(res, 0, 0, 0);
    }

    @Test
    public void mustRejectPutsForSameIdButDifferentLinks() throws Throwable {
        fixture.put(NODE_001, "key1", "value1");

        expectedException.expect(LinkMismatchException.class);
        fixture.put(new Node(NODE_001.getId(), "fakelink"), "key2", "value2");
    }

    @Test
    public void mustRejectGetsForSameIdButDifferentLinks() throws Throwable {
        fixture.put(NODE_001, "key1", "value1");

        expectedException.expect(LinkMismatchException.class);
        fixture.get(new Node(NODE_001.getId(), "fakelink"), "key1");
    }

    @Test
    public void mustRejectGetAllsForSameIdButDifferentLinks() throws Throwable {
        fixture.put(NODE_001, "key1", "value1");

        expectedException.expect(LinkMismatchException.class);
        fixture.getAll(new Node(NODE_001.getId(), "fakelink"));
    }


    @Test
    public void mustRejectRemovesForSameIdButDifferentLinks() throws Throwable {
        fixture.put(NODE_001, "key1", "value1");

        expectedException.expect(LinkMismatchException.class);
        fixture.get(new Node(NODE_001.getId(), "fakelink"), "key1");
    }

    @Test
    public void mustRejectRemoveAllsForSameIdButDifferentLinks() throws Throwable {
        fixture.put(NODE_001, "key1", "value1");

        expectedException.expect(LinkMismatchException.class);
        fixture.removeAll(new Node(NODE_001.getId(), "fakelink"));
    }
}
