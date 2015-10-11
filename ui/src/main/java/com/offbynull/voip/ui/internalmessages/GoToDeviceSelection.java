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
