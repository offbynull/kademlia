package com.offbynull.voip.audio;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import com.offbynull.voip.audio.internalmessages.CloseDevicesRequest;
import com.offbynull.voip.audio.internalmessages.ErrorResponse;
import com.offbynull.voip.audio.internalmessages.LoadDevicesRequest;
import com.offbynull.voip.audio.internalmessages.LoadDevicesResponse;
import com.offbynull.voip.audio.internalmessages.LoadDevicesResponse.InputDevice;
import com.offbynull.voip.audio.internalmessages.LoadDevicesResponse.OutputDevice;
import com.offbynull.voip.audio.internalmessages.OpenDevicesRequest;
import com.offbynull.voip.audio.internalmessages.SuccessResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioFormat;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AudioRunnable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(AudioRunnable.class);

    private static final int SAMPLE_RATE = 8000; //8k samples per second
    private static final int NUM_CHANNELS = 1; // mono
    private static final int SAMPLE_SIZE = 8; // 1 channel (mono), so 8-bits per sample

    // frame = set of samples for all channels at a given point in time
    private static final int FRAME_SIZE = NUM_CHANNELS * SAMPLE_SIZE / 8; // 1 byte per frame (because we're 1 chan (mono) & 8bits per chan)
    private static final int FRAME_RATE = FRAME_SIZE * SAMPLE_RATE;

    private static final AudioFormat EXPECTED_FORMAT = new AudioFormat(PCM_SIGNED, SAMPLE_RATE, SAMPLE_SIZE, NUM_CHANNELS, FRAME_SIZE,
            FRAME_RATE, true);

    private final Bus bus;
    private final Map<String, Shuttle> outgoingShuttles;

    private int nextDeviceId;
    private Map<Integer, LineEntry> inputDevices;
    private Map<Integer, LineEntry> outputDevices;
    
    private TargetDataLine openInputDevice;
    private Thread inputReadThread;
    
    private SourceDataLine openOutputDevice;
    private LinkedBlockingQueue<OutputData> outputQueue;
    private Thread outputWriteThread;

    public AudioRunnable(Bus bus) {
        Validate.notNull(bus);
        this.bus = bus;
        outgoingShuttles = new HashMap<>();

        inputDevices = new HashMap<>();
        outputDevices = new HashMap<>();
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

                        if (payload instanceof LoadDevicesRequest) {
                            Object response = loadDevices();
                            sendMessage(dst, src, response);
                        } else if (payload instanceof OpenDevicesRequest) {
                            Object response = openDevices((OpenDevicesRequest) payload);
                            sendMessage(dst, src, response);
                        } else if (payload instanceof CloseDevicesRequest) {
                            Object response = closeDevices();
                            sendMessage(dst, src, response);
                        } else {
                            LOG.error("Unrecognized message: {}", payload);
                        }
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
            closeDevices();
            bus.close();
        }
    }

    private Object loadDevices() {
        Map<Integer, LineEntry> newOutputDevices = new HashMap<>();
        Map<Integer, LineEntry> newInputDevices = new HashMap<>();
        List<OutputDevice> respOutputDevices = new LinkedList<>();
        List<InputDevice> respInputDevices = new LinkedList<>();

        Mixer.Info[] mixerInfos;
        try {
            mixerInfos = AudioSystem.getMixerInfo();
        } catch (Exception e) {
            LOG.error("Unable to get mixers", e);
            return new ErrorResponse("Unable to get mixers: " + e);
        }
        
        for (Mixer.Info info : mixerInfos) {
            Mixer mixer;
            try {
                mixer = AudioSystem.getMixer(info);
            } catch (Exception e) {
                LOG.error("Unable to get mixer", e);
                continue;
            }
                

            Line.Info[] lineInfos;
            Map<Integer, LineEntry> newEntries;

            System.out.println("\n\n");
            System.out.println(info.getName());

            // out devices
            try {
                lineInfos = mixer.getSourceLineInfo(new Line.Info(SourceDataLine.class));
                newEntries = generateLineEntriesFromJavaSound(mixer, lineInfos);
                newOutputDevices.putAll(newEntries);
                newEntries.entrySet().forEach(x -> {
                    OutputDevice outputDevice = new OutputDevice(x.getKey(), x.getValue().getMixer().toString());
                    respOutputDevices.add(outputDevice);
                });
            } catch (LineUnavailableException lue) {
                LOG.error("Unable to get line from mixer", lue);
            }

            // in devices
            try {
                lineInfos = mixer.getTargetLineInfo(new Line.Info(TargetDataLine.class));
                newEntries = generateLineEntriesFromJavaSound(mixer, lineInfos);
                newInputDevices.putAll(newEntries);
                newEntries.entrySet().forEach(x -> {
                    InputDevice inputDevice = new InputDevice(x.getKey(), x.getValue().getMixer().toString());
                    respInputDevices.add(inputDevice);
                });
            } catch (LineUnavailableException lue) {
                LOG.error("Unable to get line from mixer", lue);
            }
        }
        
        
        inputDevices = newOutputDevices;
        outputDevices = newInputDevices;
        return new LoadDevicesResponse(respOutputDevices, respInputDevices);
    }
    
    private Object openDevices(OpenDevicesRequest request) {
        if (outputDevices == null || inputDevices == null) {
            return new ErrorResponse("Devices not loaded");
        }
        
        if (openOutputDevice == null || openInputDevice == null) {
            return new ErrorResponse("Devices already open");
        }
        
        int outputId = request.getOutputId();
        int inputId = request.getInputId();
        
        LineEntry outputLineEntry = outputDevices.get(outputId);
        if (outputLineEntry == null) {
            LOG.error("Input device not available: {}", outputId);
            return new ErrorResponse("Input device " + outputId + " not available");
        }
        
        LineEntry inputLineEntry = inputDevices.get(inputId);
        if (inputLineEntry == null) {
            LOG.error("Input device not available: {}", inputId);
            return new ErrorResponse("Input device " + inputId + " not available");
        }



        
        // open input device
        try {
            openInputDevice = (TargetDataLine) AudioSystem.getLine(inputLineEntry.getLineInfo());
            openInputDevice.open(EXPECTED_FORMAT);
        } catch (Exception e) {
            openInputDevice = null;
            LOG.error("Unable to open input device", e);
            return new ErrorResponse("Unable to open input device");
        }

        
        
        
        // open output device
        try {
            openOutputDevice = (SourceDataLine) AudioSystem.getLine(inputLineEntry.getLineInfo());
            openOutputDevice.open(EXPECTED_FORMAT);
        } catch (Exception e) {
            try {
                openInputDevice.close();
            } catch (Exception innerE) {
                LOG.error("Unable to close input device", innerE);
            }
            
            openInputDevice = null;
            openOutputDevice = null;
            LOG.error("Unable to open output device", e);
            return new ErrorResponse("Unable to open output device");
        }
        
        
        
        
        // start input read thread
        InputReadRunnable inputReadRunnable = new InputReadRunnable(openInputDevice, bus);
        inputReadThread = new Thread(inputReadRunnable);
        inputReadThread.setDaemon(true);
        inputReadThread.setName(getClass().getSimpleName() + "-" + inputReadRunnable.getClass().getSimpleName());
        inputReadThread.start();
        
        
        
        
        // start input read thread
        outputQueue = new LinkedBlockingQueue<>();
        OutputWriteRunnable outputWriteRunnable = new OutputWriteRunnable(openOutputDevice, outputQueue);
        outputWriteThread = new Thread(outputWriteRunnable);
        outputWriteThread.setDaemon(true);
        outputWriteThread.setName(getClass().getSimpleName() + "-" + outputWriteRunnable.getClass().getSimpleName());
        outputWriteThread.start();

        
        
        
        return new SuccessResponse();
    }
    
    private Object closeDevices() {
        if (openOutputDevice == null) {
            try {
                openOutputDevice.close();
            } catch (Exception innerE) {
                LOG.error("Unable to close output device", innerE);
            }
            
            openOutputDevice = null;
            outputWriteThread.interrupt();
            outputWriteThread = null;
            outputQueue = null;
        }
        
        if (openInputDevice == null) {
            try {
                openInputDevice.close();
            } catch (Exception innerE) {
                LOG.error("Unable to close input device", innerE);
            }
            
            openInputDevice = null;
            inputReadThread.interrupt();
        }

        return new SuccessResponse();
    }

    private Map<Integer, LineEntry> generateLineEntriesFromJavaSound(Mixer m, Line.Info[] lineInfos) throws LineUnavailableException {
        Map<Integer, LineEntry> entries = new HashMap<>();
        for (Line.Info lineInfo : lineInfos) {
            if (lineInfo instanceof DataLine.Info) {
                DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                AudioFormat[] forms = dataLineInfo.getFormats();
                for (AudioFormat form : forms) {
                    if (form.matches(EXPECTED_FORMAT)) {
                        entries.put(nextDeviceId, new LineEntry(m, lineInfo));
                    }
                }
            }
        }
        return entries;
    }

    private void sendMessage(Address dst, Address src, Object response) {
        String dstPrefix = dst.getElement(0);
        Shuttle shuttle = outgoingShuttles.get(dstPrefix);
        
        if (shuttle != null) {
            shuttle.send(Collections.singleton(new Message(src, dst, response)));
        } else {
            LOG.warn("Unable to find shuttle for outgoing response: {}", response);
        }
    }
    
    
    private static final class LineEntry {

        private final Mixer mixer;
        private final Line.Info lineInfo;

        public LineEntry(Mixer mixer, Line.Info lineInfo) {
            Validate.notNull(mixer);
            Validate.notNull(lineInfo);
            this.mixer = mixer;
            this.lineInfo = lineInfo;
        }

        public Mixer getMixer() {
            return mixer;
        }

        public Line.Info getLineInfo() {
            return lineInfo;
        }

    }
}
