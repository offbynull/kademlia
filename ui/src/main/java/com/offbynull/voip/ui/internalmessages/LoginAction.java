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
 * The user has performed an action to log in to the network.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class LoginAction {
    private final String username;
    private final String bootstrap;

    /**
     * Constructs a {@link LoginAction} object.
     * @param username login username
     * @param bootstrap bootstrap link to use to join the network
     */
    public LoginAction(String username, String bootstrap) {
        Validate.notNull(username);
        Validate.notNull(bootstrap);
        this.username = username;
        this.bootstrap = bootstrap;
    }

    /**
     * Get the username.
     * @return login username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the bootstrap link.
     * @return link to use to join the network
     */
    public String getBoostrap() {
        return bootstrap;
    }
    
}
