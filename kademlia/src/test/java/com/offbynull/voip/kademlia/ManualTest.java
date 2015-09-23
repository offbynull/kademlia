package com.offbynull.voip.kademlia;

import com.offbynull.peernetic.core.actor.ActorRunner;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.core.gateways.direct.DirectGateway;
import com.offbynull.voip.kademlia.internalmessages.Start;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import java.util.Arrays;
import java.util.Scanner;
import org.apache.commons.io.Charsets;
import org.junit.Test;

public final class ManualTest {

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_DIRECT_ADDRESS_STRING = "direct";
    private static final String BASE_LOG_ADDRESS_STRING = "log";
    
    private static final Address BASE_ACTOR_ADDRESS = Address.of(BASE_ACTOR_ADDRESS_STRING);
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_DIRECT_ADDRESS = Address.of(BASE_DIRECT_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);
    
    @Test
    public void main() throws Exception {
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        DirectGateway directGateway = new DirectGateway(BASE_DIRECT_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        ActorRunner actorRunner = new ActorRunner(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        directGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        
        actorRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(directGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());

        // Seed node
        addNode("111", null, actorRunner);
        
        // Connecting nodes
        addNode("000", "111", actorRunner);

        Scanner in = new Scanner(System.in);
        while (true) {
//            System.out.print("Enter node to search for: ");
            String idStr = in.nextLine();
            
//            DO SOMETHING HERE
        }
    }

    private static void addNode(String idStr, String bootstrapIdStr, ActorRunner actorRunner) {
        Id id = Id.create(idStr);
        Node bootstrapNode = bootstrapIdStr == null ? null : new Node(Id.create(bootstrapIdStr), bootstrapIdStr);
        
        byte[] seed1 = Arrays.copyOf(idStr.getBytes(Charsets.US_ASCII), MIN_SEED_SIZE);
        byte[] seed2 = Arrays.copyOf(idStr.getBytes(Charsets.US_ASCII), MIN_SEED_SIZE);
        
        actorRunner.addActor(
                idStr,
                new KademliaCoroutine(),
                new Start(
                        new SimpleAddressTransformer(BASE_ACTOR_ADDRESS),
                        id,
                        bootstrapNode,
                        seed1,
                        seed2,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }
}
