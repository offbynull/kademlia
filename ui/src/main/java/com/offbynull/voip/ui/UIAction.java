package com.offbynull.voip.ui;

final class UIAction {
    private Object message;

    public UIAction(Object message) {
        this.message = message;
    }

    public Object getMessage() {
        return message;
    }
    
}
