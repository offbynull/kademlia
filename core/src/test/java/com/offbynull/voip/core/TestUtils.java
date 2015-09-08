package com.offbynull.voip.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
        
        Set<Node> actual = changeSet.viewAdded().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetRemoved(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        Set<Node> actual = changeSet.viewRemoved().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }

    public static void verifyChangeSetUpdated(ChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewUpdated().size());
        
        Set<Node> actual = changeSet.viewUpdated().stream().map(x -> x.getNode()).collect(Collectors.toSet());
        Set<Node> expected = new HashSet<>(Arrays.asList(nodes));
        
        assertEquals(expected, actual);
    }
}
