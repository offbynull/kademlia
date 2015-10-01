package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.voip.kademlia.AddressConstants.JOIN_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_ADVERTISE_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_EXT_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_INT_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_REFRESH_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_RELATIVE_ADDRESS;
import com.offbynull.voip.kademlia.internalmessages.Kill;
import com.offbynull.voip.kademlia.internalmessages.Start;
import com.offbynull.voip.kademlia.internalmessages.Start.KademliaParameters;
import com.offbynull.voip.kademlia.model.Id;
import org.apache.commons.lang3.Validate;

public final class KademliaSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;

    public KademliaSubcoroutine(Address subAddress) {
        Validate.notNull(subAddress);
        this.subAddress = subAddress;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerAddress();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        Id baseId = start.getBaseId();
        String bootstrapLink = start.getBootstrapLink();
        KademliaParameters kademliaParameters = start.getKademliaParameters();
        byte[] seed1 = start.getSeed1();
        byte[] seed2 = start.getSeed2();
        AddressTransformer addressTransformer = start.getAddressTransformer();

        State state = new State(
                timerAddress,
                graphAddress,
                logAddress,
                seed1,
                seed2,
                baseId,
                kademliaParameters,
                addressTransformer);

        state.getGraphHelper().createGraphs(ctx);

        try {
            // Join (or just initialize if no bootstrap node is set)
            JoinSubcoroutine joinTask = new JoinSubcoroutine(
                    subAddress.appendSuffix(JOIN_RELATIVE_ADDRESS),
                    state,
                    bootstrapLink);
            joinTask.run(cnt);

            // Create maintanence tasks that are supposed to run in parallel
            SubcoroutineRouter router = new SubcoroutineRouter(subAddress.appendSuffix(ROUTER_RELATIVE_ADDRESS), ctx);
            Controller controller = router.getController();

            controller.add(
                    new AdvertiseSubcoroutine(
                            subAddress.appendSuffix(ROUTER_ADVERTISE_RELATIVE_ADDRESS),
                            state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new RefreshSubcoroutine(
                            subAddress.appendSuffix(ROUTER_REFRESH_RELATIVE_ADDRESS),
                            state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new ExternalRequestHandlerSubcoroutine(
                            subAddress.appendSuffix(ROUTER_EXT_HANDLER_RELATIVE_ADDRESS),
                            state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new InternalRequestHandlerSubcoroutine(
                            subAddress.appendSuffix(ROUTER_INT_HANDLER_RELATIVE_ADDRESS),
                            state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);

            // Process messages
            while (true) {
                cnt.suspend();
                
                boolean forwardedToRouter = router.forward().isForwarded();
                if (!forwardedToRouter) {
                    Object msg = ctx.getIncomingMessage();
                    boolean isFromSelf = ctx.getSource().equals(ctx.getSelf());
                    if (isFromSelf && msg instanceof Kill) {
                        throw new RuntimeException("Kill message encountered");
                    }
                }
            }
        } catch (Exception e) {
            ctx.addOutgoingMessage(logAddress, error("Shutting down client {} -- {}", ctx.getSelf(), e));
            throw e;
        }
    }
}
