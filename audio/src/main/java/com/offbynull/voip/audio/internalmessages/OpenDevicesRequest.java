package com.offbynull.voip.audio.internalmessages;

public final class OpenDevicesRequest {
    private final int outputId;
    private final int inputId;

    public OpenDevicesRequest(int outputId, int inputId) {
        this.outputId = outputId;
        this.inputId = inputId;
    }

    public int getOutputId() {
        return outputId;
    }

    public int getInputId() {
        return inputId;
    }
    
}
