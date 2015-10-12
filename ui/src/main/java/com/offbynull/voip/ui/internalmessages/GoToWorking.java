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
 * Changes the UI to the working screen (the screen in which you're waiting for something to happen).
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToWorking {
    private final String message;

    /**
     * Constructs a {@link GoToWorking} object.
     * @param message text to display while waiting
     * @throws NullPointerException if any argument is {@code null}
     */
    public GoToWorking(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    /**
     * Get message.
     * @return text to display while waiting
     */
    public String getMessage() {
        return message;
    }
    
}
