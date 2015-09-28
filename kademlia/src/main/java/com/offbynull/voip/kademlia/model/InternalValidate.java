package com.offbynull.voip.kademlia.model;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class InternalValidate {

    private InternalValidate() {
        // do nothing
    }

    static void correctState(Node node, boolean condition) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(node != null);

        if (!condition) {
            throw new BadNodeStateException(node);
        }
    }

    static void exists(Node expectedNode, NodeLeastRecentSet nodeSet) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(expectedNode != null);
        Validate.validState(nodeSet != null);

        Node node = nodeSet.get(expectedNode.getId());
        if (node == null) {
            throw new NodeNotFoundException(expectedNode);
        } else if (!node.getLink().equals(expectedNode.getLink())) {
            Validate.validState(node.getId().equals(expectedNode.getId())); // sanity check
            throw new LinkMismatchException(node, expectedNode.getLink());
        }
    }

    static void matchesLength(int expectedLength, Id id) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(id != null);
        Validate.validState(expectedLength > 0); // ids will always be 1 bit or greater

        // matching if the bitlengths match?
        if (expectedLength != id.getBitLength()) {
            throw new IdLengthMismatchException(id, expectedLength);
        }
    }

    static void forwardTime(Instant previousTime, Instant currentTime) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(previousTime != null);
        Validate.validState(currentTime != null);

        if (currentTime.isBefore(previousTime)) {
            throw new BackwardTimeException(previousTime, currentTime);
        }
    }

    static void matchesPrefix(BitString expectedPrefix, Id id) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(id != null);
        Validate.validState(expectedPrefix != null);

        // matching if the bitlengths match?
        if (id.getBitString().getSharedPrefixLength(expectedPrefix) != expectedPrefix.getBitLength()) {
            throw new IdPrefixMismatchException(id, expectedPrefix);
        }
    }

    static void matchesLink(Node expectedNode, Node actualNode) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(expectedNode != null);
        Validate.validState(actualNode != null);
        Validate.validState(expectedNode.getId().equals(actualNode.getId())); // ids must be equal for this method to be called

        if (!expectedNode.getLink().equals(actualNode.getLink())) {
            // if ID exists but link for ID is different
            throw new LinkMismatchException(actualNode, expectedNode.getLink());
        }
    }

    static void notMatchesBase(Id baseId, Id inputId) {
        // throws illegalstateexception, because if you made it to this point you should never encounter these conditions
        Validate.validState(baseId != null);
        Validate.validState(inputId != null);
        // Validate.validState(baseNode.getId().getBitLength() == inputNode.getId().getBitLength()); // not required

        if (baseId.equals(inputId)) {
            throw new BaseIdMatchException(baseId);
        }
    }
}
