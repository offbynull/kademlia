package com.offbynull.voip.ui.internalmessages;

public final class UpdateMessageRate {
    private final int incomingMessagesPerSecond;
    private final int outgoingMessagesPerSecond;

    public UpdateMessageRate(int incomingMessagesPerSecond, int outgoingMessagesPerSecond) {
        this.incomingMessagesPerSecond = incomingMessagesPerSecond;
        this.outgoingMessagesPerSecond = outgoingMessagesPerSecond;
    }

    public int getIncomingMessagesPerSecond() {
        return incomingMessagesPerSecond;
    }

    public int getOutgoingMessagesPerSecond() {
        return outgoingMessagesPerSecond;
    }
    
}
