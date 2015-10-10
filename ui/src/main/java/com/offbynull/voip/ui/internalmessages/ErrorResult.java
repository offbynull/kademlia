package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

public final class ErrorResult {
    private final String message;

    public ErrorResult(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorResult{" + "message=" + message + '}';
    }
    
}
