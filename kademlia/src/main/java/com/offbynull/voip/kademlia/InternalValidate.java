package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

final class InternalValidate {

    private InternalValidate() {
        // do nothing
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
