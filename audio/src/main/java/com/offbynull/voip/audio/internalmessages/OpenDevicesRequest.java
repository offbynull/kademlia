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

/**
 * Opens an audio output device and an audio input device.
 * <p>
 * Responses sent to this request are ...
 * <ul>
 * <li>{@link SuccessResponse}</li>
 * <li>{@link ErrorResponse}</li>
 * </ul>
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class OpenDevicesRequest {
    private final int outputId;
    private final int inputId;

    /**
     * Constructs a {@link OpenDevicesRequest} object.
     * @param outputId id of output device to open
     * @param inputId id of input device to open
     */
    public OpenDevicesRequest(int outputId, int inputId) {
        this.outputId = outputId;
        this.inputId = inputId;
    }

    /**
     * Get ID of output device to open.
     * @return ID of output device to open
     */
    public int getOutputId() {
        return outputId;
    }

    /**
     * Get ID of input device to open.
     * @return ID of input device to open
     */
    public int getInputId() {
        return inputId;
    }
    
}
