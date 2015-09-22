package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.externalmessages.FindRequest;
import com.offbynull.voip.kademlia.externalmessages.FindResponse;
import com.offbynull.voip.kademlia.externalmessages.PingRequest;
import com.offbynull.voip.kademlia.externalmessages.PingResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address logAddress;
    private final AddressTransformer addressTransformer;
    
    private final Router router;

    public IncomingMessageHandlerSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.logAddress = state.getLogAddress();
        this.addressTransformer = state.getAddressTransformer();
        
        this.router = state.getRouter();
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            cnt.suspend();
            
            Object msg = ctx.getIncomingMessage();
            
            if (ctx.getSelf().isPrefixOf(ctx.getSource())) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Message from self ({}) ignored: {}", ctx.getSource(), msg));
                continue;
            }

//            if (!(msg instanceof KademliaRequest)) {
//                ctx.addOutgoingMessage(subAddress, logAddress, info("Incorrect message typeignored: {}", msg));
//                continue;
//            }
//            
//            KademliaRequest baseReq = (KademliaRequest) msg;
//            String srcLink = addressTransformer.remoteAddressToLinkId(ctx.getSource());
//            Id srcId = baseReq.getFromId();
//            Node srcNode = new Node(srcId, srcLink);
//            
//            router.touch(ctx.getTime(), srcNode);
            
            if (msg instanceof PingRequest) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming ping request from {}", ctx.getSource()));
                
                PingResponse resp = new PingResponse();
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), resp);
                ctx.addOutgoingMessage(subAddress, logAddress, info("Responding with: {}", resp));
            } else if (msg instanceof FindRequest) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming find request from {}", ctx.getSource()));

                FindRequest req = (FindRequest) msg;
                Id findId = req.getFindId();
                List<Node> foundNodes = router.find(findId, 20);

                FindResponse resp = new FindResponse(foundNodes.toArray(new Node[foundNodes.size()]));
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), resp);
                ctx.addOutgoingMessage(subAddress, logAddress, info("Responding with closest nodes: {}", foundNodes));
            } else {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Unknown request from {}: {}", ctx.getSource(), msg));
            }
        }
    }

}
