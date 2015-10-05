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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.lang3.Validate;

public final class LoadDevicesResponse {
    
    private final UnmodifiableList<OutputDevice> outputDevices;
    private final UnmodifiableList<InputDevice> inputDevices;

    public LoadDevicesResponse(List<OutputDevice> outputDevices, List<InputDevice> inputDevices) {
        Validate.notNull(outputDevices);
        Validate.notNull(inputDevices);
        Validate.noNullElements(outputDevices);
        Validate.noNullElements(inputDevices);
        
        this.outputDevices = (UnmodifiableList<OutputDevice>) UnmodifiableList.unmodifiableList(new ArrayList<>(outputDevices));
        this.inputDevices = (UnmodifiableList<InputDevice>) UnmodifiableList.unmodifiableList(new ArrayList<>(inputDevices));
    }

    public UnmodifiableList<OutputDevice> getOutputDevices() {
        return outputDevices;
    }

    public UnmodifiableList<InputDevice> getInputDevices() {
        return inputDevices;
    }
    
    public static final class OutputDevice {
        private final int id;
        private final String name;

        public OutputDevice(int id, String name) {
            Validate.notNull(name);
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "OutputDevice{" + "id=" + id + ", name=" + name + '}';
        }
        
    }

    public static final class InputDevice {
        private final int id;
        private final String name;

        public InputDevice(int id, String name) {
            Validate.notNull(name);
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "InputDevice{" + "id=" + id + ", name=" + name + '}';
        }
        
    }
}
