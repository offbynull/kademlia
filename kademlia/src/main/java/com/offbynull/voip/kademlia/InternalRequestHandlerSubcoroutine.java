/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.internalmessages.Kill;
import com.offbynull.voip.kademlia.internalmessages.SearchRequest;
import com.offbynull.voip.kademlia.internalmessages.SearchResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class InternalRequestHandlerSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;
    
    private final Address logAddress;

    public InternalRequestHandlerSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.state = state;
        
        this.logAddress = state.getLogAddress();
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Address routerAddress = subAddress.appendSuffix("router");
        SubcoroutineRouter subcoroutineRouter = new SubcoroutineRouter(routerAddress, ctx);
        Controller subcoroutineRouterController = subcoroutineRouter.getController();
        
        Map<Subcoroutine<?>, Address> responseAddresses = new HashMap<>();
        while (true) {
            cnt.suspend();
            
            ForwardResult fr = subcoroutineRouter.forward();
            if (fr.isForwarded()) {
                // Message was for one of our subcoroutines running in subcoroutineRouter
                if (fr.isCompleted()) {
                    @SuppressWarnings("unchecked")
                    List<Node> nodes = (List<Node>) fr.getResult();
                    Subcoroutine<?> subcoroutine = fr.getSubcoroutine();
                    
                    Node[] nodesArr = nodes.toArray(new Node[nodes.size()]);
                    Address responseAddress = responseAddresses.get(subcoroutine);
                    
                    ctx.addOutgoingMessage(subAddress, responseAddress, new SearchResponse(nodesArr));
                    ctx.addOutgoingMessage(subAddress, logAddress, info("Search request completed: {}", nodes));
                }
            } else {
                // Message was for us
                Object msg = ctx.getIncomingMessage();

                if (msg instanceof SearchRequest) {
                    SearchRequest req = (SearchRequest) msg;
                    Id findId = req.getFindId();
                    int maxResults = req.getMaxResults();

                    ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming search request for: {}", findId));

                    // make sure ignoreSelf == true, we never want to return ourself even if our id is the one being searched for
                    FindSubcoroutine findSubcoroutine
                            = new FindSubcoroutine(routerAddress.appendSuffix("find"), state, findId, maxResults, true, true);
                    subcoroutineRouterController.add(findSubcoroutine, AddBehaviour.ADD_PRIME);
                    responseAddresses.put(findSubcoroutine, ctx.getSource());
                } else if (msg instanceof Kill) {
                    ctx.addOutgoingMessage(subAddress, logAddress, info("Incoming kill request"));
                    throw new RuntimeException("Kill message encountered");
                } else {
                    ctx.addOutgoingMessage(subAddress, logAddress, info("Unknown request from {}: {}", ctx.getSource(), msg));
                }
            }
        }
    }

}
