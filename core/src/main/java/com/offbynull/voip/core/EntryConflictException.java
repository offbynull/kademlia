package com.offbynull.voip.core;

import org.apache.commons.lang3.Validate;

public class EntryConflictException extends Exception {
    private static final long serialVersionUID = 1L;

    private final Entry existingEntry;

    public EntryConflictException(Entry existingEntry) {
        Validate.notNull(existingEntry);
        this.existingEntry = existingEntry;
    }

    public Entry getExistingEntry() {
        return existingEntry;
    }
}
