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

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

final class InputData {
    private final byte[] data;

    public InputData(byte[] data, int len) {
        Validate.notNull(data);
        Validate.isTrue(len >= 0);
        Validate.isTrue(len <= data.length);
        this.data = Arrays.copyOf(data, len);
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
    
}
