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
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.peernetic.core.shuttles.simple.SimpleShuttle;
import com.offbynull.voip.ui.internalmessages.ReadyAction;
import java.util.function.Supplier;
import javafx.scene.Parent;
import org.apache.commons.lang3.Validate;

/**
 * {@link Gateway} that provides a HTML5 interface rendered through JavaFX.
 * <p>
 * The gateway is initialized once its component gets added as part of a JavaFX scene. Once initialized, it sends out a {@link ReadyAction}
 * message to the address that it's designated to send UI events to (remember to assign shuttles BEFORE adding the gateway to a JavaFX
 * scene graph). Once initialized, you can change the UI and be notified of user interaction by messages in {@link com.offbynull.voip.ui}.
 * <p>
 * To get access to the JavaFX component, use {@link #getJavaFXComponent() }.
 * @author Kasra Faghihi
 */
public final class UiGateway implements InputGateway, OutputGateway {

    private final Thread thread;
    private final Bus bus;
    private final Bus busToWebRegion; // bus to UI component

    private final SimpleShuttle shuttle;

    private final SingleSupplier<Parent> webRegionSupplier;

    /**
     * Constructs a {@link UIGateway} instance.
     * @param prefix address prefix for this gateway
     * @param dstAddress address to send UI events to
     * @throws NullPointerException if any argument is {@code null}
     */
    public UiGateway(String prefix, Address dstAddress) {
        Validate.notNull(prefix);
        Validate.notNull(dstAddress);

        bus = new Bus();

        busToWebRegion = new Bus();
        webRegionSupplier = new SingleSupplier<>(() -> UiWebRegion.create(busToWebRegion, bus));
        
        shuttle = new SimpleShuttle(prefix, bus);
        thread = new Thread(new UiRunnable(shuttle.getPrefix(), dstAddress, bus, webRegionSupplier, busToWebRegion));
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

    /**
     * Get this gateway's JavaFX component.
     * <p>
     * Note that this method returns a {@link Supplier} that generates a component, not the component itself. As such, it should be safe to
     * invoke this method outside of a JavaFX context, as long as the returned supplier's {@link Supplier#get() } method is invoked from
     * within a JavaFX context.
     * <p>
     * Also note that the returned supplier's {@link Supplier#get() } method can only be invoked once. Subsequent invocations will result in
     * an {@link IllegalStateException}.
     * @return supplier that generates the JavaFX component for this gateway
     */
    public Supplier<Parent> getJavaFXComponent() {
        return webRegionSupplier;
    }
}
