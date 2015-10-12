/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.ui.internalmessages;

import org.apache.commons.lang3.Validate;

/**
 * Changes the UI to the error screen (the screen in which an error message is shown).
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToError {
    private final String message;
    private final boolean unrecoverable;
    
    /**
     * Constructs a {@link GoToUnrecoverableError} object.
     * @param message error text to display
     * @param unrecoverable error was critical (unrecoverable)
     * @throws NullPointerException if any argument is {@code null}
     */
    public GoToError(String message, boolean unrecoverable) {
        Validate.notNull(message);
        this.message = message;
        this.unrecoverable = unrecoverable;
    }

    /**
     * Get the message.
     * @return error text to display
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get if this is a critical error (one which cannot be recovered from).
     * @return {@code true} if error is critical, {@code false} otherwise
     */
    public boolean isUnrecoverable() {
        return unrecoverable;
    }
    
}
