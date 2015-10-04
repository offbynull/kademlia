package com.offbynull.voip.audio;

import java.util.LinkedList;
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
                LinkedList<OutputData> buffers = dumpQueue();
                if (buffers.size() > 1) { // more than 1 buffer, show a warning
                    LOG.info("Excess number of output buffers read: {} -- only playing last", buffers.size());
                }
                
                byte[] dataBytes = buffers.getLast().getData();
                try {
                    openOutputDevice.write(dataBytes, 0, dataBytes.length);
                } catch (IllegalArgumentException iae) {
                    LOG.warn("Output buffer potentially malformed: {}", iae.toString());
                }
            }
        } catch (Exception e) {
            LOG.info("Output thread stopped: {}", e.toString());
        }
    }
    
    private LinkedList<OutputData> dumpQueue() throws InterruptedException {
        LinkedList<OutputData> ret = new LinkedList<>();
        
        OutputData first = outputQueue.take();
        ret.add(first);
        outputQueue.drainTo(ret);
        
        return ret;
    }
}
