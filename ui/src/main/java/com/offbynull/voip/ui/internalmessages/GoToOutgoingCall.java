package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class GoToOutgoingCall {
    private final String username;

    public GoToOutgoingCall(String username) {
        Validate.notNull(username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    
}
