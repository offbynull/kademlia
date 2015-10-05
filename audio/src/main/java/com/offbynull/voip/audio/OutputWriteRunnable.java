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

import java.util.Iterator;
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
    private final int bufferSize;

    public OutputWriteRunnable(SourceDataLine openOutputDevice, LinkedBlockingQueue<OutputData> outputQueue, int bufferSize) {
        Validate.notNull(openOutputDevice);
        Validate.notNull(outputQueue);
        Validate.isTrue(bufferSize > 0);
        this.openOutputDevice = openOutputDevice;
        this.outputQueue = outputQueue;
        this.bufferSize = bufferSize;
    }

    @Override
    public void run() {
        LOG.info("Output thread started: {}", openOutputDevice);
        try {
            byte[] internalBuffer = new byte[bufferSize];
            
            while (true) {
                LinkedList<OutputData> readBuffers = dumpQueue();
                Iterator<OutputData> readBuffersIt = readBuffers.descendingIterator();
                int remainingAmount = internalBuffer.length;
                int requiredAmount = 0;
                int copyAmount = 0;
                while (remainingAmount > 0 && readBuffersIt.hasNext()) {
                    OutputData readBuffer = readBuffersIt.next();
                    byte[] readBufferData = readBuffer.getData();
                    requiredAmount = readBufferData.length;
                    
                    copyAmount = Math.min(remainingAmount, requiredAmount);
                    int copyFrom = requiredAmount - copyAmount;
                    int copyTo = remainingAmount - copyAmount;
                    
                    System.arraycopy(readBufferData, copyFrom, internalBuffer, copyTo, copyAmount);
                    
                    remainingAmount -= copyAmount;
                }
                
                if (copyAmount != requiredAmount || readBuffersIt.hasNext()) { // more than 1 buffer or some data not copied, show a warning
                    LOG.info("Excess data read: {} buffers -- only playing last {} bytes", readBuffers.size(), bufferSize);
                }
                
                
                try {
                    openOutputDevice.write(internalBuffer, remainingAmount, internalBuffer.length - remainingAmount);
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
