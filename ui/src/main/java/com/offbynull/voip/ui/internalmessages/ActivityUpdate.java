package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class ActivityUpdate {
    private final int incomingMessageRate;
    private final int outgoingMessageRate;

    public ActivityUpdate(int incomingMessageRate, int outgoingMessageRate) {
        Validate.isTrue(incomingMessageRate >= 0);
        Validate.isTrue(outgoingMessageRate >= 0);
        this.incomingMessageRate = incomingMessageRate;
        this.outgoingMessageRate = outgoingMessageRate;
    }

    public int getIncomingMessageRate() {
        return incomingMessageRate;
    }

    public int getOutgoingMessageRate() {
        return outgoingMessageRate;
    }
    
}
