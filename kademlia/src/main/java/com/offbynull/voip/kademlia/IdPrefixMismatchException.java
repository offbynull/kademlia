package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

public final class IdPrefixMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Id id;
    private final BitString expectedPrefix;

    public IdPrefixMismatchException(Id id, BitString expectedPrefix) {
        super("ID prefix mismatch (required " + expectedPrefix +  "): " + id + ")");
        Validate.notNull(id);
        Validate.notNull(expectedPrefix);
        
        // what's the point of throwing an exception for not having a shared prefix if you have a shared prefix?
        Validate.isTrue(id.getBitString().getSharedPrefixLength(expectedPrefix) != expectedPrefix.getBitLength());
        this.id = id;
        this.expectedPrefix = expectedPrefix;
    }

    public Id getId() {
        return id;
    }

    public BitString getExpectedPrefix() {
        return expectedPrefix;
    }


}
