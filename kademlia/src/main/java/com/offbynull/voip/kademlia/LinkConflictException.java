package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

public class LinkConflictException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Entry existingEntry;

    public LinkConflictException(Entry existingEntry) {
        Validate.notNull(existingEntry);
        this.existingEntry = existingEntry;
    }

    public Entry getExistingEntry() {
        return existingEntry;
    }
}
