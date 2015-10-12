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

/**
 * Changes the UI to the log in screen.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToLogin {
    private final boolean reset;

    /**
     * Constructs a {@link GoToLogin} object.
     * @param reset if {@code true} resets previously set fields on the login page (username and bootstrap link), {@code false} keeps them
     * intact
     */
    public GoToLogin(boolean reset) {
        this.reset = reset;
    }

    /**
     * Get reset flag.
     * @return {@code true} to reset previously set fields on the login page (username and bootstrap link), {@code false} to keep them
     * intact
     */
    public boolean isReset() {
        return reset;
    }
    
}
