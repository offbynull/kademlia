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

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.gateway.OutputGateway;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.security.SecureRandom;
import org.apache.commons.lang3.Validate;

public final class AudioGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    
    private final SimpleShuttle shuttle;

    /**
     * Constructs a {@link DirectGateway} instance.
     * @param prefix address prefix for this gateway
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalStateException if failed to initialize the underlying {@link SecureRandom} object used for message ids
     */
    public AudioGateway(String prefix) {
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
