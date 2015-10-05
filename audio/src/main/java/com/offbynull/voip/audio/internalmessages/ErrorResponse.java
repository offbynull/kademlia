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
package com.offbynull.voip.audio.internalmessages;

import org.apache.commons.lang3.Validate;

/**
 * Generic error response.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class ErrorResponse {
    private final String message;

    /**
     * Constructs a {@link ErrorResponse} object.
     * @param message error message
     * @throws NullPointerException if any argument is {@code null}
     */
    public ErrorResponse(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    /**
     * Get error message.
     * @return error message
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" + "message=" + message + '}';
    }
    
}
