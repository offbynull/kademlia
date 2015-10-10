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

import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.gateway.InputGateway;
import com.offbynull.peernetic.core.gateway.OutputGateway;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javafx.scene.Parent;
import org.apache.commons.lang3.Validate;

public final class UIGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    private final Bus busToJavaFX; // bus to UI component

    private final SimpleShuttle shuttle;

    private final Supplier<Parent> javaFxComponent;

    public UIGateway(String prefix) {
        Validate.notNull(prefix);

        bus = new Bus();
        busToJavaFX = new Bus();

        AtomicBoolean consumedComponent = new AtomicBoolean(false);
        javaFxComponent = () -> {
            // you need a supplier because you can't create a webregion outside of a javafx context
            // if you try to call supplier.get() outside of a javafx context, it'll throw an exception
            // if you try to call supplier.get() more than once, it'll throw an illegalstateexception
            Validate.validState(consumedComponent.compareAndSet(false, true), "Consumed already -- only allowed to call get() once");
            return WebRegion.create(bus, busToJavaFX);
        };
        
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new UIRunnable(bus));
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

    public Supplier<Parent> getJavaFXComponent() {
        return javaFxComponent;
    }
}
