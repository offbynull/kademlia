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
