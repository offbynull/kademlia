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
 * The UI is ready.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class ReadyAction {
    private final boolean failed;

    /**
     * Constructs a {@link ReadyAction} object.
     * @param failed {@code true} if loading the UI failed, {@code false} otherwise
     */
    public ReadyAction(boolean failed) {
        this.failed = failed;
    }

    /**
     * Get the loading state.
     * @return {@code true} if loading the UI failed, {@code false} otherwise
     */
    public boolean isFailed() {
        return failed;
    }
    
}
