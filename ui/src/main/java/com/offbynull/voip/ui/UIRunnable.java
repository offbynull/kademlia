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
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UIRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(UIRunnable.class);

    private final Bus bus;
    private final Map<String, Shuttle> outgoingShuttles;

    public UIRunnable(Bus bus) {
        Validate.notNull(bus);
        this.bus = bus;
        outgoingShuttles = new HashMap<>();
    }

    @Override
    public void run() {
        try {
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

//                        if (payload instanceof LoadDevicesRequest) {
//                            Object response = loadDevices();
//                            sendMessage(src, dst, response);
//                        } else if (payload instanceof OpenDevicesRequest) {
//                            Object response = openDevices(src, dst, (OpenDevicesRequest) payload);
//                            sendMessage(src, dst, response);
//                        } else if (payload instanceof CloseDevicesRequest) {
//                            Object response = closeDevices();
//                            sendMessage(src, dst, response);
//                        } else if (payload instanceof OutputPCMBlock) {
//                            if (openedToAddress == null || openedFromAddress == null) {
//                                LOG.warn("Output PCM block received but devices closed");
//                                continue;
//                            }
//                            
//                            OutputPCMBlock outputPCMBlock = (OutputPCMBlock) payload;
//                            byte[] data = outputPCMBlock.getData();
//                            OutputData outputData = new OutputData(data);
//                            
//                            outputQueue.put(outputData);
//                        } else {
//                            LOG.error("Unrecognized message: {}", payload);
//                        }
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

    private void sendMessage(Address to, Address from, Object response) {
        String dstPrefix = to.getElement(0);
        Shuttle shuttle = outgoingShuttles.get(dstPrefix);
        
        if (shuttle != null) {
            shuttle.send(Collections.singleton(new Message(from, to, response)));
        } else {
            LOG.warn("Unable to find shuttle for outgoing response: {}", response);
        }
    }
    
    
}
