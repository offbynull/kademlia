package com.offbynull.voip.kademlia.model;

import org.apache.commons.lang3.Validate;

public final class IdLengthMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Id id;
    private final int expectedLength;

    public IdLengthMismatchException(Id id, int expectedBitLength) {
        super("ID bitlength size mismatch (required " + expectedBitLength +  "): " + id + ")");
        Validate.notNull(id);
        Validate.isTrue(expectedBitLength > 0); // ids will always be 1 bit or greater
        Validate.isTrue(expectedBitLength != id.getBitLength()); // what's the point of throwing an exception for the bitlengths
                                                                            // not matching if the bitlengths match?
        this.id = id;
        this.expectedLength = expectedBitLength;
    }

    public Id getId() {
        return id;
    }

    public int getExpectedLength() {
        return expectedLength;
    }

}
