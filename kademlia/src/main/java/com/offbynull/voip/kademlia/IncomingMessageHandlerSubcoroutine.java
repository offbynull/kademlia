package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;import com.offbynull.voip.kademlia.externalmessages.FindRequest;
import com.offbynull.voip.kademlia.externalmessages.FindResponse;
import com.offbynull.voip.kademlia.externalmessages.PingRequest;
import com.offbynull.voip.kademlia.externalmessages.PingResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class IncomingMessageHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address logAddress;
    private final State state;

    public IncomingMessageHandlerSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.logAddress = state.getLogAddress();
        this.state = state;
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
            
            if (msg instanceof PingRequest) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming ping request from {}", ctx.getSource()));
                
                PingResponse resp = new PingResponse();
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), resp);
            } else if (msg instanceof FindRequest) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming find request from {}", ctx.getSource()));

                FindRequest req = (FindRequest) msg;
                
                Id findId = req.getId();
                List<Node> foundNodes = state.getRouter().find(findId, 20);

                FindResponse resp = new FindResponse(foundNodes.toArray(new Node[foundNodes.size()]));
                ctx.addOutgoingMessage(subAddress, ctx.getSource(), resp);
                ctx.addOutgoingMessage(subAddress, logAddress, info("Closest nodes found: {}", foundNodes));
            } else {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Unknown request from {}: {}", ctx.getSource(), msg));
            }
        }
    }

}
