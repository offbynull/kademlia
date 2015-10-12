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
 * Changes the UI to the incoming call screen.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToIncomingCall {
    private final String username;

    /**
     * Constructs a {@link GoToIncomingCall} message.
     * @param username username calling
     */
    public GoToIncomingCall(String username) {
        Validate.notNull(username);
        this.username = username;
    }

    /**
     * Get the caller's username.
     * @return username calling
     */
    public String getUsername() {
        return username;
    }
    
}
