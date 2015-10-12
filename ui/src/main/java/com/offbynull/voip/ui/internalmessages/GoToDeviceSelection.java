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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

/**
 * Changes the UI to the audio input/output device selection screen.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class GoToDeviceSelection {
    private final Map<Integer, String> inputDevices;
    private final Map<Integer, String> outputDevices;

    /**
     * Constructs a {@link GoToDeviceSelection} object.
     * @param inputDevices map of audio input devices (key = id, value = name)
     * @param outputDevices map of audio output devices (key = id, value = name)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public GoToDeviceSelection(Map<Integer, String> inputDevices, Map<Integer, String> outputDevices) {
        Validate.notNull(inputDevices);
        Validate.notNull(outputDevices);
        Validate.noNullElements(inputDevices.keySet());
        Validate.noNullElements(outputDevices.keySet());
        Validate.noNullElements(inputDevices.values());
        Validate.noNullElements(outputDevices.values());
        this.inputDevices = new HashMap<>(inputDevices);
        this.outputDevices = new HashMap<>(outputDevices);
    }

    /**
     * Get audio input devices.
     * @return map of audio input devices (key = id, value = name)
     */
    public Map<Integer, String> getInputDevices() {
        return new HashMap<>(inputDevices);
    }

    /**
     * Get audio output devices.
     * @return map of audio output devices (key = id, value = name)
     */
    public Map<Integer, String> getOutputDevices() {
        return new HashMap<>(outputDevices);
    }
    
}
