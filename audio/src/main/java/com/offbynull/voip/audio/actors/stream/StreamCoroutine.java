package com.offbynull.voip.audio.actors.stream;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.audio.actors.stream.internalmessages.Start;
import org.apache.commons.lang3.Validate;

public final class StreamCoroutine implements Subcoroutine<Void> {

    private final Address subAddress;

    public StreamCoroutine(Address subAddress) {
        Validate.notNull(subAddress);
        
        this.subAddress = subAddress;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        AddressTransformer addressTransformer = start.getAddressTransformer();
        Address audioIoAddress = start.getAudioIoAddress();
        Address timerAddress = start.getTimerAddress();
        Address logAddress = start.getLogAddress();

        State state = new State(
                addressTransformer,
                audioIoAddress,
                timerAddress,
                logAddress);

        try {
            // Process messages
            while (true) {
                cnt.suspend();
            }
        } catch (Exception e) {
            ctx.addOutgoingMessage(logAddress, error("Shutting down client {} -- {}", ctx.getSelf(), e));
            throw e;
        }
    }
    
}
