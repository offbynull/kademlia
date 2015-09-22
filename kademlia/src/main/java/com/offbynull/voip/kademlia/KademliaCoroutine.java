package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.error;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import static com.offbynull.voip.kademlia.AddressConstants.JOIN_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_ADVERTISE_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_EXT_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_INT_HANDLER_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_REFRESH_RELATIVE_ADDRESS;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_RELATIVE_ADDRESS;
import com.offbynull.voip.kademlia.internalmessages.Start;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;

public final class KademliaCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerAddress = start.getTimerAddress();
        Address graphAddress = start.getGraphAddress();
        Address logAddress = start.getLogAddress();
        Id baseId = start.getBaseId();
        Node bootstrapNode = start.getBootstrapNode();
        byte[] seed1 = start.getSeed1();
        byte[] seed2 = start.getSeed2();
        AddressTransformer addressTransformer = start.getAddressTransformer();


        try {
            State state = new State(
                    timerAddress,
                    graphAddress,
                    logAddress,
                    seed1,
                    seed2,
                    baseId,
                    2,
                    20,
                    addressTransformer);

            // Join (or just initialize if no bootstrap node is set)
            JoinSubcoroutine joinTask = new JoinSubcoroutine(JOIN_RELATIVE_ADDRESS, state, bootstrapNode);
            joinTask.run(cnt);

            // Create maintanence tasks that are supposed to run in parallel
            SubcoroutineRouter router = new SubcoroutineRouter(ROUTER_RELATIVE_ADDRESS, ctx);
            Controller controller = router.getController();

            controller.add(
                    new AdvertiseSubcoroutine(ROUTER_ADVERTISE_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new RefreshSubcoroutine(ROUTER_REFRESH_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(
                    new ExternalRequestHandlerSubcoroutine(ROUTER_EXT_HANDLER_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD);
            controller.add(
                    new InternalRequestHandlerSubcoroutine(ROUTER_INT_HANDLER_RELATIVE_ADDRESS, state),
                    AddBehaviour.ADD);

            // Process messages
            while (true) {
                cnt.suspend();
                router.forward();
            }
        } catch (Exception e) {
            ctx.addOutgoingMessage(logAddress, error("Shutting down client {} -- {}", ctx.getSelf(), e));
        }
    }
}
