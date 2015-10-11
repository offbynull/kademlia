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

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Parent;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UIRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UIRunnable.class);

    private final String selfPrefix;
    private final Address dstAddress;
    
    private final Bus bus;
    private final Map<String, Shuttle> outgoingShuttles;
    
    private final SingleSupplier<Parent> webRegionSupplier;
    private final Bus busToWebRegion;

    public UIRunnable(String selfPrefix, Address dstAddress, Bus bus, SingleSupplier<Parent> webRegionSupplier, Bus busToWebRegion) {
        Validate.notNull(selfPrefix);
        Validate.notNull(dstAddress);
        Validate.notNull(bus);
        Validate.notNull(webRegionSupplier);
        Validate.notNull(busToWebRegion);
        Validate.isTrue(!dstAddress.isEmpty());
        
        this.selfPrefix = selfPrefix;
        this.dstAddress = dstAddress;
        
        this.bus = bus;
        outgoingShuttles = new HashMap<>();
    
        this.webRegionSupplier = webRegionSupplier;
        this.busToWebRegion = busToWebRegion;
    }

    @Override
    public void run() {
        try {
            UIWebRegion webRegion = (UIWebRegion) webRegionSupplier.retainedReference(); // doesn't return until its created
            
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects = bus.pull();
                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);

                for (Object incomingObj : incomingObjects) {
                    if (incomingObj instanceof Message) {
                        Message msg = (Message) incomingObj;

                        Address src = msg.getSourceAddress();
                        Address dst = msg.getDestinationAddress();
                        Object payload = msg.getMessage();

                        LOG.debug("Processing incoming message from {} to {}: {}", src, dst, payload);
                        busToWebRegion.add(payload);
                    } else if (incomingObj instanceof AddShuttle) {
                        AddShuttle addShuttle = (AddShuttle) incomingObj;
                        Shuttle shuttle = addShuttle.getShuttle();
                        Shuttle existingShuttle = outgoingShuttles.putIfAbsent(shuttle.getPrefix(), shuttle);
                        Validate.validState(existingShuttle == null);
                    } else if (incomingObj instanceof RemoveShuttle) {
                        RemoveShuttle removeShuttle = (RemoveShuttle) incomingObj;
                        String prefix = removeShuttle.getPrefix();
                        Shuttle oldShuttle = outgoingShuttles.remove(prefix);
                        Validate.validState(oldShuttle != null);
                    } else if (incomingObj instanceof UIAction) {
                        UIAction uiAction = (UIAction) incomingObj;
                        Object payload = uiAction.getMessage();
                        sendMessage(payload);
                    } else {
                        throw new IllegalStateException("Unexpected message type: " + incomingObj);
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Audio gateway interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }

    private void sendMessage(Object payload) {
        String dstPrefix = dstAddress.getElement(0);
        Shuttle shuttle = outgoingShuttles.get(dstPrefix);
        
        if (shuttle != null) {
            shuttle.send(Collections.singleton(new Message(Address.of(selfPrefix), dstAddress, payload)));
        } else {
            LOG.warn("Unable to find shuttle for outgoing response: {}", payload);
        }
    }
    
    
}
