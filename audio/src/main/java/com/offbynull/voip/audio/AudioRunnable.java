package com.offbynull.voip.audio;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.util.List;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AudioRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AudioRunnable.class);

    private final Bus bus;

    public AudioRunnable(Bus bus) {
        Validate.notNull(bus);
        this.bus = bus;
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
                        MultiMap<Address, Object> payloads = new MultiValueMap<>();
                        Message msg = (Message) incomingObj;

                        Address dst = msg.getDestinationAddress();
                        Object payload = msg.getMessage();
                        payloads.put(dst, payload);
                        
                        LOG.debug("Processing incoming message from {} to {}: {}", msg.getSourceAddress(), dst, payload);
                        
                    } else {
                        throw new IllegalStateException("Unexpected message type: " + incomingObj);
                    }
                }
            }
        } catch (InterruptedException ie) {
            LOG.debug("Graph gateway interrupted");
            Thread.interrupted();
        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
        } finally {
            bus.close();
        }
    }

    
    private interface MessageProcessor<T> {
        
    }
}