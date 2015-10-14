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
package com.offbynull.voip.audio.gateways.io;

import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import org.apache.commons.lang3.Validate;

final class LineEntry {
    private final Mixer mixer;
    private final Line.Info lineInfo;

    public LineEntry(Mixer mixer, Line.Info lineInfo) {
        Validate.notNull(mixer);
        Validate.notNull(lineInfo);
        this.mixer = mixer;
        this.lineInfo = lineInfo;
    }

    public Mixer getMixer() {
        return mixer;
    }

    public Line.Info getLineInfo() {
        return lineInfo;
    }
    
}
