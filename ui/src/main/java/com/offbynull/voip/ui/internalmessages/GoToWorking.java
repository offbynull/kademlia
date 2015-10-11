package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class GoToWorking {
    private final String message;

    public GoToWorking() {
        this("");
    }
    
    public GoToWorking(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}
