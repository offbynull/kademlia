package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class GoToUnrecoverableError {
    private final String message;

    public GoToUnrecoverableError() {
        this("");
    }
    
    public GoToUnrecoverableError(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}
