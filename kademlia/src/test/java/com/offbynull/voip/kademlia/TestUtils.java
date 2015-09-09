package com.offbynull.voip.kademlia;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;

final class TestUtils {
    private TestUtils() {
        // do nothing
    }
    
    public static void verifyNodeChangeSetCounts(NodeChangeSet changeSet, int expectedAdded, int expectedRemoved, int expectedUpdated) {
        assertEquals(expectedAdded, changeSet.viewAdded().size());
        assertEquals(expectedRemoved, changeSet.viewRemoved().size());
        assertEquals(expectedUpdated, changeSet.viewUpdated().size());
    }

    public static  void verifyNodeChangeSetAdded(NodeChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewAdded().size());
        
        List<Node> actual = changeSet.viewAdded();
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyNodeChangeSetRemoved(NodeChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        List<Node> actual = changeSet.viewRemoved();
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyNodeChangeSetUpdated(NodeChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewUpdated().size());
        
        List<Node> actual = changeSet.viewUpdated();
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }
    
    public static void verifyChangeSetCounts(EntryChangeSet changeSet, int expectedAdded, int expectedRemoved, int expectedUpdated) {
        assertEquals(expectedAdded, changeSet.viewAdded().size());
        assertEquals(expectedRemoved, changeSet.viewRemoved().size());
        assertEquals(expectedUpdated, changeSet.viewUpdated().size());
    }

    public static  void verifyChangeSetAdded(EntryChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewAdded().size());
        
        List<Node> actual = changeSet.viewAdded().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetRemoved(EntryChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        List<Node> actual = changeSet.viewRemoved().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetUpdated(EntryChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewUpdated().size());
        
        List<Node> actual = changeSet.viewUpdated().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyNodesInEntries(List<Entry> entries, Node ... nodes) {
        assertEquals(entries.size(), nodes.length);
        
        List<Node> actual = entries.stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyTimeInEntries(List<Entry> entries, Instant ... instants) {
        assertEquals(entries.size(), instants.length);
        
        List<Instant> actual = entries.stream().map(x -> x.getTime()).collect(Collectors.toList());
        List<Instant> expected = Arrays.asList(instants);
        
        assertEquals(expected, actual);
    }
}
