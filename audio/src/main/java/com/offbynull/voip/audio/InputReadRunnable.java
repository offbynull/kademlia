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
package com.offbynull.voip.audio;

import com.offbynull.peernetic.core.shuttles.simple.Bus;
import javax.sound.sampled.TargetDataLine;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InputReadRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(InputReadRunnable.class);
    
    private final TargetDataLine openInputDevice;
    private final Bus bus;
    private final int bufferSize;

    public InputReadRunnable(TargetDataLine openInputDevice, Bus bus, int bufferSize) {
        Validate.notNull(openInputDevice);
        Validate.notNull(bus);
        Validate.isTrue(bufferSize > 0);
        this.openInputDevice = openInputDevice;
        this.bus = bus;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        LOG.info("Input thread started: {}", openInputDevice);
        try {
//            int bufferSize = openInputDevice.getBufferSize();
//            Validate.validState(bufferSize > 0, "Sanity check failed for input buffer size: %s", bufferSize);
            byte[] dataBytes = new byte[bufferSize];
            
            while (true) {
                int amountRead = openInputDevice.read(dataBytes, 0, dataBytes.length);
                if (amountRead != 0) { // may be 0 in some cases
                    bus.add(new InputData(dataBytes, amountRead));
                }
            }
        } catch (Exception e) {
            LOG.info("Input thread stopped: {}", e.toString());
        }
    }
    
}
