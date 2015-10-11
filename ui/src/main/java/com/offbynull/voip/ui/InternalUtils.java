package com.offbynull.voip.ui;

import java.util.Map;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

final class InternalUtils {

    public static JSObject mapToJSObject(WebEngine webEngine, Map<?, ?> map) {
        // Cannot use "{}" to create a javascript object because this will cause executeScript to return a String rather than a JSObject...
        // not sure why this is the case. Trying to put stuff inside the brackets just screws things up even more.
        //
        // "[]" will create a JSObject, but won't work if you start using keys that aren't an index.
        //
        // "new Object()" seems like the only reliable way to create a new JS object.
        JSObject ret = (JSObject) webEngine.executeScript("new Object()");
        map.entrySet().forEach(e -> ret.setMember("" + e.getKey(), e.getValue()));

        return ret;
    }
}
