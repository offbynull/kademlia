package com.offbynull.voip.ui.internalmessages;

public final class ReadyAction {
    private final boolean failed;

    public ReadyAction(boolean failed) {
        this.failed = failed;
    }

    public boolean isFailed() {
        return failed;
    }
    
}
