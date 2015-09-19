package com.offbynull.voip.kademlia;

import org.apache.commons.lang3.Validate;

public final class IdBitLengthMismatchException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Id conflictingId;
    private final int expectedBitLength;

    public IdBitLengthMismatchException(Id conflictingId, int expectedBitLength) {
        super("ID bitlength size mismatch (required " + expectedBitLength +  "): " + conflictingId + ")");
        Validate.notNull(conflictingId);
        Validate.isTrue(expectedBitLength > 0); // ids will always be 1 bit or greater
        Validate.isTrue(expectedBitLength != conflictingId.getBitLength()); // what's the point of throwing an exception for the bitlengths
                                                                            // not matching if the bitlengths match?
        this.conflictingId = conflictingId;
        this.expectedBitLength = expectedBitLength;
    }

    public Id getConflictingId() {
        return conflictingId;
    }

    public int getExpectedBitLength() {
        return expectedBitLength;
    }

}
