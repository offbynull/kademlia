package com.offbynull.voip.kademlia;

import java.time.Instant;
import org.apache.commons.lang3.Validate;

public final class BackwardTimeException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Instant previousTime;
    private final Instant inputTime;

    public BackwardTimeException(Instant previousTime, Instant inputTime) {
        super("Time is (" + inputTime + ") is before " + previousTime);
        Validate.notNull(previousTime);
        Validate.notNull(inputTime);
        // what's the point of throwing an exception for going backwards in time if you're going forward in time?
        Validate.isTrue(!inputTime.isBefore(previousTime));
        this.previousTime = previousTime;
        this.inputTime = inputTime;
    }

    public Instant getPreviousTime() {
        return previousTime;
    }

    public Instant getInputTime() {
        return inputTime;
    }
    
}
