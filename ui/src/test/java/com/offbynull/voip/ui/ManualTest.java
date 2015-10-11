package com.offbynull.voip.ui;

import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.voip.ui.internalmessages.ChooseDevicesAction;
import com.offbynull.voip.ui.internalmessages.GoToIdle;
import com.offbynull.voip.ui.internalmessages.GoToLogin;
import com.offbynull.voip.ui.internalmessages.GoToWorking;
import com.offbynull.voip.ui.internalmessages.ConnectAction;
import com.offbynull.voip.ui.internalmessages.DisconnectAction;
import com.offbynull.voip.ui.internalmessages.ShowDeviceSelection;
import com.offbynull.voip.ui.internalmessages.ReadyAction;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Parent;



public class ManualTest {

    public static void main(String[] args) throws Exception {
        UIGateway uiGateway = new UIGateway("ui", Address.of("direct"));
        DirectGateway directGateway = new DirectGateway("direct");
        
        uiGateway.addOutgoingShuttle(directGateway.getIncomingShuttle());
        directGateway.addOutgoingShuttle(uiGateway.getIncomingShuttle());
        
        Supplier<Parent> componentSupplier = uiGateway.getJavaFXComponent();
        PlaceholderApplication.start(componentSupplier);
        
        

        
        while (true) {
            List<Message> messages = directGateway.readMessages();
            for (Message message : messages) {
                Object payload = message.getMessage();
                
                if (payload instanceof ReadyAction) {
                    directGateway.writeMessage(Address.of("ui"), new GoToLogin());
                } else if (payload instanceof ConnectAction) {
                    directGateway.writeMessage(Address.of("ui"), new GoToWorking("Logging in 1..."));
                    Thread.sleep(1000L);
//                    directGateway.writeMessage(Address.of("ui"), new GoToWorking("Logging in 2..."));
//                    Thread.sleep(1000L);
//                    directGateway.writeMessage(Address.of("ui"), new GoToWorking("Logging in 3..."));
//                    Thread.sleep(1000L);
                    directGateway.writeMessage(Address.of("ui"), new GoToIdle());
                } else if (payload instanceof DisconnectAction) {
                    directGateway.writeMessage(Address.of("ui"), new GoToLogin());
                } else if (payload instanceof ChooseDevicesAction) {
                    HashMap<Integer, String> inDevices = new HashMap<>();
                    HashMap<Integer, String> outDevices = new HashMap<>();
                    
                    inDevices.put(0, "indevice 1");
                    inDevices.put(1, "indevice 2");

                    outDevices.put(0, "outdevice 1");
                    outDevices.put(1, "outdevice 2");

                    directGateway.writeMessage(Address.of("ui"), new ShowDeviceSelection(inDevices, outDevices));
                }
            }
        }
    }

}
