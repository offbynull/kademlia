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

import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.gateway.OutputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import com.offbynull.voip.audio.gateways.io.internalmessages.CloseDevicesRequest;
import com.offbynull.voip.audio.gateways.io.internalmessages.InputPCMBlock;
import com.offbynull.voip.audio.gateways.io.internalmessages.LoadDevicesRequest;
import com.offbynull.voip.audio.gateways.io.internalmessages.OpenDevicesRequest;
import com.offbynull.voip.audio.gateways.io.internalmessages.OutputPCMBlock;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that reads in from an audio input device (e.g. microphone) and writes out to an audio output device (e.g. speakers).
 * <p>
 * To initialize this gateway, you must load available audio devices by sending {@link LoadDevicesRequest}. Once initialized, you can open
 * an audio input device and audio output device pair using {@link OpenDevicesRequest}.
 * <p>
 * Once the devices have been opened ...
 * <ul>
 * <li>The gateway will send you {@link InputPCMBlock}s with PCM data read in from the audio input device (sent to the address that opened
 * the devices).</li>
 * <li>You send {@link OutputPCMBlock} to the gateway with PCM data to write out to the audio output device.</li>
 * </ul>
 * <p>
 * Use {@link CloseDevicesRequest} to close the devices you opened.
 * @author Kasra Faghihi
 */
public final class AudioIOGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    /**
     * Constructs a {@link AudioIOGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     */
    public AudioIOGateway(String prefix) {
        Validate.notNull(prefix);
        
        bus = new Bus();
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new AudioRunnable(bus));
        thread.setDaemon(true);
        thread.setName(getClass().getSimpleName() + "-" + prefix);
        thread.start();
    }

    @Override
    public Shuttle getIncomingShuttle() {
        return shuttle;
    }

    @Override
    public void addOutgoingShuttle(Shuttle shuttle) {
        Validate.notNull(shuttle);
        bus.add(new AddShuttle(shuttle));
    }

    @Override
    public void removeOutgoingShuttle(String shuttlePrefix) {
        Validate.notNull(shuttlePrefix);
        bus.add(new RemoveShuttle(shuttlePrefix));
    }

    @Override
    public void close() throws InterruptedException {
        thread.interrupt();
        thread.join();
    }
}
