package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class GoToLogin {
    private final String message;
    private final boolean reset;

    public GoToLogin() {
        this("", true);
    }
    
    public GoToLogin(String message, boolean reset) {
        Validate.notNull(message);
        this.message = message;
        this.reset = reset;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReset() {
        return reset;
    }
    
}
