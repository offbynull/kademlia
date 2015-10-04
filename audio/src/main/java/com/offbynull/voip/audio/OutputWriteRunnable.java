package com.offbynull.voip.audio;

import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.SourceDataLine;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OutputWriteRunnable implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(OutputWriteRunnable.class);
    
    private final SourceDataLine openOutputDevice;
    private final LinkedBlockingQueue<OutputData> outputQueue;

    public OutputWriteRunnable(SourceDataLine openOutputDevice, LinkedBlockingQueue<OutputData> outputQueue) {
        Validate.notNull(openOutputDevice);
        Validate.notNull(outputQueue);
        this.openOutputDevice = openOutputDevice;
        this.outputQueue = outputQueue;
    }

    @Override
    public void run() {
        LOG.info("Output thread started: {}", openOutputDevice);
        try {
            while (true) {
                OutputData data = outputQueue.take();
                byte[] dataBytes = data.getData();
                openOutputDevice.write(dataBytes, 0, dataBytes.length);
            }
        } catch (Exception e) {
            LOG.info("Output thread stopped: {}", e.toString());
        }
    }
    
}
