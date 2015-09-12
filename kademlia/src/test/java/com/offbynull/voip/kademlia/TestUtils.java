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
    
    public static void verifyActivityChangeSetCounts(ActivityChangeSet changeSet, int expectedAdded, int expectedRemoved,
            int expectedUpdated) {
        assertEquals(expectedAdded, changeSet.viewAdded().size());
        assertEquals(expectedRemoved, changeSet.viewRemoved().size());
        assertEquals(expectedUpdated, changeSet.viewUpdated().size());
    }

    public static  void verifyActivityChangeSetAdded(ActivityChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewAdded().size());
        
        List<Node> actual = changeSet.viewAdded().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyActivityChangeSetRemoved(ActivityChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewRemoved().size());
        
        List<Node> actual = changeSet.viewRemoved().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyActivityChangeSetUpdated(ActivityChangeSet changeSet, Node ... nodes) {
        assertEquals(nodes.length, changeSet.viewUpdated().size());
        
        List<Node> actual = changeSet.viewUpdated().stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyNodes(List<Node> inputNodes, Node ... testAgainstNodes) {
        assertEquals(inputNodes.size(), testAgainstNodes.length);
        
        List<Node> actual = inputNodes;
        List<Node> expected = Arrays.asList(testAgainstNodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyNodesInActivities(List<Activity> activities, Node ... nodes) {
        assertEquals(activities.size(), nodes.length);
        
        List<Node> actual = activities.stream().map(x -> x.getNode()).collect(Collectors.toList());
        List<Node> expected = Arrays.asList(nodes);
        
        assertEquals(expected, actual);
    }

    public static void verifyTimeInActivities(List<Activity> activities, Instant ... instants) {
        assertEquals(activities.size(), instants.length);
        
        List<Instant> actual = activities.stream().map(x -> x.getTime()).collect(Collectors.toList());
        List<Instant> expected = Arrays.asList(instants);
        
        assertEquals(expected, actual);
    }
}
