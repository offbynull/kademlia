package com.offbynull.voip.audio.test;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.voip.audio.AudioGateway;
import com.offbynull.voip.audio.internalmessages.CloseDevicesRequest;
import com.offbynull.voip.audio.internalmessages.InputPCMBlock;
import com.offbynull.voip.audio.internalmessages.LoadDevicesRequest;
import com.offbynull.voip.audio.internalmessages.LoadDevicesResponse;
import com.offbynull.voip.audio.internalmessages.OpenDevicesRequest;
import com.offbynull.voip.audio.internalmessages.OutputPCMBlock;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public final class ManualTest {

    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_AUDIO_ADDRESS_STRING = "audio";
    private static final String BASE_DIRECT_ADDRESS_STRING = "direct";
    private static final String BASE_LOG_ADDRESS_STRING = "log";

    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_AUDIO_ADDRESS = Address.of(BASE_AUDIO_ADDRESS_STRING);
    private static final Address BASE_DIRECT_ADDRESS = Address.of(BASE_DIRECT_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);

    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();
        
        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        AudioGateway audioGateway = new AudioGateway(BASE_AUDIO_ADDRESS_STRING);
        DirectGateway directGateway = new DirectGateway(BASE_DIRECT_ADDRESS_STRING);

        
        graphGateway.addStage(() -> new ConsoleStage());
        ConsoleStage consoleStage = ConsoleStage.getInstance();
        
        
        audioGateway.addOutgoingShuttle(directGateway.getIncomingShuttle());
        directGateway.addOutgoingShuttle(audioGateway.getIncomingShuttle());


        consoleStage.outputLine("load -- Load audio devices");
        consoleStage.outputLine("open <inputid> <outputid> -- Open input/output audio device");
        consoleStage.outputLine("close -- Close audio device");
        consoleStage.outputLine("exit -- Exit");
        
        Thread ioPump = null;
        top:
        while (true) {
            ArrayBlockingQueue<String> outputQueue = new ArrayBlockingQueue<>(1);
            consoleStage.outputLine("Enter command");
            consoleStage.setCommandProcessor((input) -> {
                outputQueue.add(input);
                return "Executing... " + input;
            });
            
            String command = outputQueue.take();
            Scanner scanner = new Scanner(command);
            
            switch (scanner.next()) {
                case "load": {
                    directGateway.writeMessage(BASE_AUDIO_ADDRESS, new LoadDevicesRequest());
                    LoadDevicesResponse resp = (LoadDevicesResponse) directGateway.readMessages().get(0).getMessage();
                    
                    consoleStage.outputLine("Input Devices");
                    resp.getInputDevices().stream().map(x -> x.toString()).forEach(consoleStage::outputLine);
                    
                    consoleStage.outputLine("Output Devices");
                    resp.getOutputDevices().stream().map(x -> x.toString()).forEach(consoleStage::outputLine);
                    break;
                }
                case "open": {
                    int inputId = scanner.nextInt();
                    int outputId = scanner.nextInt();
                    directGateway.writeMessage(BASE_AUDIO_ADDRESS, new OpenDevicesRequest(outputId, inputId));
                    Object resp = directGateway.readMessages().get(0).getMessage();
                    consoleStage.outputLine(resp.toString());
                    
                    if (ioPump != null) {
                        consoleStage.outputLine("Audio IO pump already started");
                        break;
                    }
                    
                    consoleStage.outputLine("Starting audio IO pump");
                    ioPump = new Thread(() -> {
                        try {
                            while (true) {
                                List<Message> recvMsgs = directGateway.readMessages();

                                Message[] sendMsgs = recvMsgs.stream()
                                        .map(m -> new Message(
                                                m.getDestinationAddress(),
                                                m.getSourceAddress(),
                                                new OutputPCMBlock(((InputPCMBlock) m.getMessage()).getData())))
                                        .toArray(x -> new Message[x]);

                                consoleStage.outputLine(sendMsgs.length + " pumped");

                                directGateway.writeMessages(sendMsgs);
                            }
                        } catch (Exception e) {
                            consoleStage.outputLine("Audio IO pump crashed" + e);
                        }
                    });
                    ioPump.setDaemon(true);
                    ioPump.setName("Audio IO Pump");
                    ioPump.start();
                    break;
                }
                case "close": {
                    if (ioPump != null) {
                        ioPump.interrupt();
                        ioPump.join();
                        ioPump = null;
                    }
                    
                    Thread.sleep(500L);
                    
                    directGateway.writeMessage(BASE_AUDIO_ADDRESS, new CloseDevicesRequest());
                    Object resp = directGateway.readMessages().get(0).getMessage();
                    consoleStage.outputLine(resp.toString());
                    break;
                }
                case "exit": {
                    GraphGateway.exitApplication();
                    break top;
                }
                default:
                    consoleStage.outputLine("Unrecognized command");
            }
        }
        
        GraphGateway.awaitShutdown();
    }
}
