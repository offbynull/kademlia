package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class GoToLogin {
    private final String message;

    public GoToLogin() {
        this("");
    }
    
    public GoToLogin(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getErrorMessage() {
        return message;
    }
    
}
