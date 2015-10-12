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
 * The user has performed an action to change the audio input/output device.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class ChooseDevicesAction {
    private final int inputId;
    private final int outputId;

    /**
     * Constructs a {@link ChooseDevicesAction} object.
     * @param inputId the desired id of the audio input device
     * @param outputId the desired id of the audio output device
     */
    public ChooseDevicesAction(int inputId, int outputId) {
        this.inputId = inputId;
        this.outputId = outputId;
    }

    /**
     * Get the desired audio input device id.
     * @return id of the desired audio input device
     */
    public int getInputId() {
        return inputId;
    }

    /**
     * Get the desired audio output device id.
     * @return id of the desired audio output device
     */
    public int getOutputId() {
        return outputId;
    }
}
