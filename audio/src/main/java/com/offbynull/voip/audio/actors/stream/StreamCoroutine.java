package com.offbynull.voip.audio.actors.stream;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
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
        return null;
    }
    
}
