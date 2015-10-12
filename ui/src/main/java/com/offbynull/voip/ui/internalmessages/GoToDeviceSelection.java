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

public final class GoToDeviceSelection {
    private final Map<Integer, String> inputDevices;
    private final Map<Integer, String> outputDevices;

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

    public Map<Integer, String> getInputDevices() {
        return new HashMap<>(inputDevices);
    }

    public Map<Integer, String> getOutputDevices() {
        return new HashMap<>(outputDevices);
    }
    
}
