package com.offbynull.voip.audio.test;

import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import com.offbynull.voip.audio.AudioGateway;
import com.offbynull.voip.audio.internalmessages.LoadDevicesRequest;
import com.offbynull.voip.audio.internalmessages.LoadDevicesResponse;
import java.util.Scanner;
import java.util.StringJoiner;
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
                case "load":
                    directGateway.writeMessage(BASE_AUDIO_ADDRESS, new LoadDevicesRequest());
                    LoadDevicesResponse resp = (LoadDevicesResponse) directGateway.readMessages().get(0).getMessage();
                    
                    consoleStage.outputLine("Input Devices");
                    resp.getInputDevices().stream().map(x -> x.toString()).forEach(consoleStage::outputLine);
                    
                    consoleStage.outputLine("Output Devices");
                    resp.getOutputDevices().stream().map(x -> x.toString()).forEach(consoleStage::outputLine);
                    break;
                case "open":
                    consoleStage.outputLine("Not implemented");
                    break;
                case "close":
                    consoleStage.outputLine("Not implemented");
                    break;
                case "exit":
                    GraphGateway.exitApplication();
                    break top;
                default:
                    consoleStage.outputLine("Unrecognized command");
            }
        }
        
        GraphGateway.awaitShutdown();
    }
}
