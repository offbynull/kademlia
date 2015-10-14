package com.offbynull.voip.audio.actors.stream.externalmessages;

import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class EstablishCallRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String callerUsername;

    public EstablishCallRequest(String callerUsername) {
        Validate.notNull(callerUsername);
        this.callerUsername = callerUsername;
    }

    public String getCallerUsername() {
        return callerUsername;
    }
    
}
