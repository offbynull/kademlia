package com.offbynull.voip.ui;

import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.voip.ui.internalmessages.GoToLogin;
import com.offbynull.voip.ui.internalmessages.GoToWorking;
import com.offbynull.voip.ui.internalmessages.LoginAction;
import com.offbynull.voip.ui.internalmessages.ReadyAction;
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
                } else if (payload instanceof LoginAction) {
                    directGateway.writeMessage(Address.of("ui"), new GoToWorking("Logging in..."));
                }
            }
        }
    }

}
