package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class CallAction {
    private final String username;

    public CallAction(String username) {
        Validate.notNull(username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
    
}
