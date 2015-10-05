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
