package com.offbynull.voip.kademlia;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.AddNode;
import com.offbynull.peernetic.visualizer.gateways.graph.LabelNode;
import com.offbynull.peernetic.visualizer.gateways.graph.MoveNode;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import com.offbynull.voip.kademlia.model.BitString;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Router;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class GraphHelper {
    
    private static final double Y_SPREAD = 50.0;
    private static final double X_SPREAD = 50.0;
    
    private final Id baseId;
    private final Address graphAddress;
    private final List<BitString> prefixes;

    public GraphHelper(Id baseId, Address graphAddress, Router router) {
        Validate.notNull(baseId);
        Validate.notNull(graphAddress);
        Validate.notNull(router);
        this.baseId = baseId;
        this.graphAddress = graphAddress.appendSuffix(baseId.getBitString().toString());
        
        List<BitString> prefixes = router.dumpBucketPrefixes();
        Collections.sort(prefixes, (x, y) -> Integer.compare(x.getBitLength(), y.getBitLength()));
        this.prefixes = Collections.unmodifiableList(prefixes);
    }

    public void addPrefixNodesToGraph(Context ctx) {
        BitString parentId = BitString.createFromString("");
        Point parentPoint = new Point(0.0, 0.0);
        
        Map<BitString, Point> processedPrefixes = new HashMap<>(); // prefix -> position on graph
        addRootToGraph(ctx, parentId, processedPrefixes, parentPoint);
     
        
        LinkedList<BitString> tempPrefixes = new LinkedList<>(prefixes);

        while (true) {
            // Get next prefixes
            ArrayList<BitString> nextLevelPrefixes = removePrefixesForNextLevel(tempPrefixes);
            if (nextLevelPrefixes.isEmpty()) {
                break;
            }
            
            // Find parent
            int bitLengthOfNextLevelPrefixes = nextLevelPrefixes.get(0).getBitLength();
            parentId = getParentPrefix(processedPrefixes.keySet(), bitLengthOfNextLevelPrefixes);
            parentPoint = processedPrefixes.get(parentId);
            
            // Calculate number of bits after prefix
            int newBitsOffset = parentId.getBitLength();
            int newBitsLength = bitLengthOfNextLevelPrefixes - parentId.getBitLength();
            
            // Calculate starting x and y positions
            double numBranches = 1 << newBitsLength;
            double missingBitLength = baseId.getBitLength() - bitLengthOfNextLevelPrefixes;
            double ySpreadAtLevel = Y_SPREAD * (missingBitLength + 1.0);
            double xSpreadAtLevel = X_SPREAD * (missingBitLength + 1.0);
            double yPosition = parentPoint.y + ySpreadAtLevel; // nodes right below the parent
            double xPosition = parentPoint.x - xSpreadAtLevel * (numBranches - 1.0) / 2.0; // nodes x-centered on parent

            // special-case for netLevelPrefixes where prefix for our baseId doesn't exist... this is the branch that falls further down
            //
            // e.g. If you're 000, prefixes will be 1, 01, 001, 000... but for each of those you'll want to show the falling-thru prefix as
            // well.. for example...
            //
            //                                    /\
            //        (NOT STATED IN PREFIXES) 0 /  \ 1
            //                                  /\
            //     (NOT STATED IN PREFIXES) 00 /  \ 01
            //                                /\
            //      (EXISTS IN PREFIXES) 000 /  \ 001
            //
            // Note that 000 exists, but 00 and 0 don't.
            BitString baseIdPortion = baseId.getBitString().getBits(0, bitLengthOfNextLevelPrefixes);
            if (!nextLevelPrefixes.contains(baseIdPortion)) {
                nextLevelPrefixes.add(baseIdPortion);
            }
            
            // Make sure smallest branch is always to the left-most by sorting
            Collections.sort(nextLevelPrefixes,
                    (x, y) -> Long.compare(
                            x.getBitsAsLong(0, x.getBitLength()),
                            y.getBitsAsLong(0, y.getBitLength())
                    )
            );
            
            // Add prefixes from routing tree
            for (BitString nextPrefix : nextLevelPrefixes) {
                addPrefixToGraph(nextPrefix, newBitsOffset, newBitsLength, xPosition, yPosition, ctx, parentId, processedPrefixes);
                xPosition += xSpreadAtLevel;
            }
        }
    }

    private void addRootToGraph(Context ctx, BitString parentId, Map<BitString, Point> processedPrefixes, Point parentPoint) {
        // Root node
        ctx.addOutgoingMessage(graphAddress, new AddNode(parentId.toString()));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(parentId.toString(), 0.0, 0.0));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(parentId.toString(), 0x7F7F7F));
        processedPrefixes.put(parentId, parentPoint);
    }

    private void addPrefixToGraph(BitString nextPrefix, int newBitsOffset, int newBitsLength, double xPosition, double yPosition,
            Context ctx, BitString parentId, Map<BitString, Point> processedPrefixes) {
        BitString displayBits = nextPrefix.getBits(newBitsOffset, newBitsLength);
        Point displayPoint = new Point(xPosition, yPosition);
        ctx.addOutgoingMessage(graphAddress, new AddNode(nextPrefix.toString()));
        ctx.addOutgoingMessage(graphAddress, new LabelNode(nextPrefix.toString(), displayBits.toString()));
        ctx.addOutgoingMessage(graphAddress, new MoveNode(nextPrefix.toString(), displayPoint.x, displayPoint.y));
        ctx.addOutgoingMessage(graphAddress, new StyleNode(nextPrefix.toString(), 0x7F7F7F));
        ctx.addOutgoingMessage(graphAddress, new AddEdge(parentId.toString(), nextPrefix.toString()));
        ctx.addOutgoingMessage(graphAddress, new StyleEdge(parentId.toString(), nextPrefix.toString(), 0x7F7F7F, 1.0));
        processedPrefixes.put(nextPrefix, displayPoint);
    }
    
    private BitString getParentPrefix(Set<BitString> addedPrefixes, int prefixLength) {
        BitString checkBitString = baseId.getBitString().getBits(0, prefixLength);
        
        while (checkBitString.getBitLength() >= 0) {
            if (addedPrefixes.contains(checkBitString)) {
                return checkBitString;
            }
            checkBitString = checkBitString.getBits(0, checkBitString.getBitLength() - 1);
        }
        
        throw new IllegalStateException(); // should never happen
    }
    
    private ArrayList<BitString> removePrefixesForNextLevel(LinkedList<BitString> sortedPrefixes) {
        ArrayList<BitString> ret = new ArrayList<>();
        
        if (sortedPrefixes.isEmpty()) {
            return ret;
        }
        
        int hitCount = sortedPrefixes.peekFirst().getBitLength();
        
        while (!sortedPrefixes.isEmpty()) {
            if (sortedPrefixes.peekFirst().getBitLength() == hitCount) {
                ret.add(sortedPrefixes.removeFirst());
            } else {
                break;
            }
        }
        
        return ret;
    }
    
    private static final class Point {
        private final double x;
        private final double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
    }
}
