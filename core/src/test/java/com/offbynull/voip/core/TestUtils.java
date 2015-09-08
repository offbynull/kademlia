package com.offbynull.voip.core;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;

final class TestUtils {
    private TestUtils() {
        // do nothing
    }
    
    public static void verifyChangeSetCounts(ChangeSet changeSet, int expectedAdded, int expectedRemoved, int expectedUpdated) {
        assertEquals(expectedAdded, changeSet.viewAdded().size());
        assertEquals(expectedRemoved, changeSet.viewRemoved().size());
        assertEquals(expectedUpdated, changeSet.viewUpdated().size());
    }

    public static  void verifyChangeSetAdded(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewAdded().size());
        
        List<Node> actual = changeSet.viewAdded().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetRemoved(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        List<Node> actual = changeSet.viewRemoved().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetUpdated(ChangeSet changeSet, Node ... nodes) {
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
        
        List<Instant> actual = entries.stream().map(x -> x.getLastSeenTime()).collect(Collectors.toList());
        List<Instant> expected = Arrays.asList(instants);
        
        assertEquals(expected, actual);
    }
}
