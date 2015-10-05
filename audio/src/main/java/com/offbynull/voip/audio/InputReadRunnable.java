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

    public InputReadRunnable(TargetDataLine openInputDevice, Bus bus) {
        Validate.notNull(openInputDevice);
        Validate.notNull(bus);
        this.openInputDevice = openInputDevice;
        this.bus = bus;
    }

    @Override
    public void run() {
        LOG.info("Input thread started: {}", openInputDevice);
        try {
            int size = openInputDevice.getBufferSize();
            Validate.validState(size > 0, "Sanity check failed for input buffer size: %s", size);
            byte[] dataBytes = new byte[size];
            
            while (true) {
                int amountRead = openInputDevice.read(dataBytes, 0, size);
                if (amountRead != 0) { // may be 0 in some cases
                    bus.add(new InputData(dataBytes, amountRead));
                }
            }
        } catch (Exception e) {
            LOG.info("Input thread stopped: {}", e.toString());
        }
    }
    
}
