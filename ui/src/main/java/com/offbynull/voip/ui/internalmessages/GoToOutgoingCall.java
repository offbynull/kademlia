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
 * Changes the UI to the outgoing call screen (the screen in which you're waiting for the person you're calling to pick up).
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToOutgoingCall {
    private final String username;

    /**
     * Constructs a {@link GoToOutgoingCall} object.
     * @param username username being called
     * @throws NullPointerException if any argument is {@code null}
     */
    public GoToOutgoingCall(String username) {
        Validate.notNull(username);
        this.username = username;
    }

    /**
     * Get the username being called.
     * @return username being called
     */
    public String getUsername() {
        return username;
    }
    
}
