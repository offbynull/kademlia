package com.offbynull.voip.ui.internalmessages;

public final class ChooseDevicesAction {
    private final int inputId;
    private final int outputId;

    public ChooseDevicesAction(int inputId, int outputId) {
        this.inputId = inputId;
        this.outputId = outputId;
    }

    public int getInputId() {
        return inputId;
    }

    public int getOutputId() {
        return outputId;
    }
}
