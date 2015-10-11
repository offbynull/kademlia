package com.offbynull.voip.ui;

import java.util.Map;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

final class InternalUtils {

    public static JSObject mapToJSObject(WebEngine webEngine, Map<?, ?> map) {
        JSObject ret = (JSObject) webEngine.executeScript("[]"); // Cannot use "{}" to create a javascript object because this will cause
                                                                 // executeScript to return a String rather than a JSObject... not sure why
                                                                 // this is the case.
        map.entrySet().forEach(e -> ret.setMember("" + e.getKey(), e.getValue()));
        
        return ret;
    }
}
