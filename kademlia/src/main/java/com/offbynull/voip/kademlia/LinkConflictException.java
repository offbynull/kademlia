package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

public class LinkConflictException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Node existingNode;

    public LinkConflictException(Node existingNode) {
        Validate.notNull(existingNode);
        this.existingNode = existingNode;
    }

    public Node getExistingNode() {
        return existingNode;
    }
}
