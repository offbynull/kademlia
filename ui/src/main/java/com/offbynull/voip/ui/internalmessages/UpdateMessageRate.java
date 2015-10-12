/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.ui.internalmessages;

/**
 * Updates the incoming/outgoing message rate (specific to the network) within the UI.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class UpdateMessageRate {
    private final int incomingMessagesPerSecond;
    private final int outgoingMessagesPerSecond;

    /**
     * Constructs a {@link UpdateMessageRate} object.
     * @param incomingMessagesPerSecond incoming message rate (messages per second)
     * @param outgoingMessagesPerSecond outgoing message rate (messages per second)
     */
    public UpdateMessageRate(int incomingMessagesPerSecond, int outgoingMessagesPerSecond) {
        this.incomingMessagesPerSecond = incomingMessagesPerSecond;
        this.outgoingMessagesPerSecond = outgoingMessagesPerSecond;
    }

    /**
     * Get the incoming message rate.
     * @return incoming message rate (messages per second)
     */
    public int getIncomingMessagesPerSecond() {
        return incomingMessagesPerSecond;
    }

    /**
     * Get the outgoing message rate.
     * @return outgoing message rate (messages per second)
     */
    public int getOutgoingMessagesPerSecond() {
        return outgoingMessagesPerSecond;
    }
    
}
