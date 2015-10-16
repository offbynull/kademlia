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
import com.offbynull.voip.audio.actors.stream.externalmessages.AudioBlock;
import com.offbynull.voip.audio.actors.stream.externalmessages.EstablishCallRequest;
import com.offbynull.voip.audio.actors.stream.externalmessages.TerminateCallRequest;
import org.apache.commons.lang3.Validate;

final class ExternalRequestHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;

    private final Address logAddress;

    public ExternalRequestHandlerSubcoroutine(Address subAddress, State state) {
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

            if (msg instanceof EstablishCallRequest) {
            } else if (msg instanceof TerminateCallRequest) {
            } else if (msg instanceof AudioBlock) {
            } else {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Unknown request from {}: {}", ctx.getSource(), msg));
            }
        }
    }

}
