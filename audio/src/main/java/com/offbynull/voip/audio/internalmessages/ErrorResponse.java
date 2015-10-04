package com.offbynull.voip.audio.internalmessages;

import org.apache.commons.lang3.Validate;

public final class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}
