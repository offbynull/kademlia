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
package com.offbynull.voip.ui;

import java.util.Map;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

final class InternalUtils {

    private InternalUtils() {
        // do nothing
    }
    
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
