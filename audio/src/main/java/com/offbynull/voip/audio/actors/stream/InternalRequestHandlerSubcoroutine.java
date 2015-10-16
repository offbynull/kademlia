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
package com.offbynull.voip.audio.actors.stream;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.audio.actors.stream.internalmessages.AcceptIncomingCall;
import com.offbynull.voip.audio.actors.stream.internalmessages.Kill;
import com.offbynull.voip.audio.actors.stream.internalmessages.OutgoingCall;
import com.offbynull.voip.audio.actors.stream.internalmessages.RejectIncomingCall;
import org.apache.commons.lang3.Validate;

final class InternalRequestHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;

    private final Address logAddress;

    public InternalRequestHandlerSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.state = state;

        this.logAddress = state.getLogAddress();
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            cnt.suspend();
            Object msg = ctx.getIncomingMessage();

            if (msg instanceof OutgoingCall) {
            } else if (msg instanceof AcceptIncomingCall) {
            } else if (msg instanceof RejectIncomingCall) {
            } else if (msg instanceof Kill) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming kill request"));
                throw new RuntimeException("Kill message encountered");
            } else {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Unknown request from {}: {}", ctx.getSource(), msg));
            }
        }
    }

}
