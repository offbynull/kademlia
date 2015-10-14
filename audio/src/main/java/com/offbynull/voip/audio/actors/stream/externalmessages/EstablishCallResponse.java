package com.offbynull.voip.audio.actors.stream.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class EstablishCallResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final State state;

    public EstablishCallResponse(State state) {
        Validate.notNull(state);
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public enum State {
        ACCEPT,
        REJECT,
        BUSY
    }
}
