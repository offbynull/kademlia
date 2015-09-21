package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.externalmessages.FindRequest;
import com.offbynull.voip.kademlia.externalmessages.FindResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.IdClosenessComparator;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import static java.lang.Math.round;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

final class FindSubcoroutine implements Subcoroutine<List<Node>> {

    private final Address subAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final AddressTransformer addressTransformer;
    private final IdGenerator idGenerator;
    
    private final Router router;
    private final Id baseId;
    private final Id findId;
    private final int maxResults;
    private final int concurrentRequests = 3;
    
    public FindSubcoroutine(Address subAddress, State state, Id findId, int maxResults) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(findId);
        Validate.isTrue(maxResults >= 0); // why would anyone ever want 0 results? let thru anyways
        
        this.subAddress = subAddress;
        
        timerAddress = state.getTimerAddress();
        logAddress = state.getLogAddress();
        addressTransformer = state.getAddressTransformer();
        idGenerator = state.getIdGenerator();
        
        router = state.getRouter();
        this.baseId = state.getBaseId();
        this.findId = findId;
        this.maxResults = maxResults;
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public List<Node> run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        // Set up subcoroutine router
        Address routerAddress = subAddress.appendSuffix("finderreq" + idGenerator.generate());
        SubcoroutineRouter msgRouter = new SubcoroutineRouter(routerAddress, ctx);
        Controller msgRouterController = msgRouter.getController();
        
        // Get initial set of nodes to query from routing table
        List<Node> startNodes = router.find(findId, maxResults);
        ctx.addOutgoingMessage(subAddress, logAddress, info("Initial route table entries closest to {}: {}", findId, startNodes));
        
        // Create sorted set of nodes to contact
        IdClosenessComparator idClosenessComparator = new IdClosenessComparator(findId);
        TreeSet<Node> contactSet = new TreeSet<>((x, y) -> idClosenessComparator.compare(x.getId(), y.getId()));
        contactSet.addAll(startNodes);

        // Create a sorted set of nodes to retain closest nodes in
        TreeSet<Node> closestSet = new TreeSet<>((x, y) -> idClosenessComparator.compare(x.getId(), y.getId()));
        
        while (true) {
            // If there's room left to query more contacts that are closer to findId, do so... 
            while (msgRouterController.size() < maxResults && !contactSet.isEmpty()) {
                // Get next farthest away node to contact
                Node contactNode = contactSet.pollLast();
                
                // If we already have maxResult closer nodes to findId, skip this node
                closestSet.add(contactNode);
                if (closestSet.size() > maxResults) {
                    Node removedNode = closestSet.pollLast();
                    if (removedNode == contactNode) {
                        continue;
                    }
                }
                
                // Initialize query
                RequestSubcoroutine<FindResponse> reqSubcoroutine = new RequestSubcoroutine.Builder<FindResponse>()
                        .sourceAddress(routerAddress, idGenerator)
                        .timerAddress(timerAddress)
                        .request(new FindRequest(findId, maxResults))
                        .addExpectedResponseType(FindResponse.class)
                        .attemptInterval(Duration.ofSeconds(10L))
                        .maxAttempts(1)
                        .throwExceptionIfNoResponse(false)
                        .build();
                
                // Add query to router
                msgRouterController.add(reqSubcoroutine, AddBehaviour.ADD_PRIME_NO_FINISH);
            }
            
            
            // Forward the current message to the router
            ForwardResult res = msgRouter.forward();
            
            // If a request completed
            if (res.isCompleted()) {
                // Get response
                FindResponse findResponse = (FindResponse) res.getResult();
                
                // Check if failure
                if (findResponse == null) {
                    DO SOMETIHNG HERE;
                }
                
                // Add node results to contacts
                Node[] nodes = findResponse.getNodes();
                contactSet.addAll(Arrays.asList(nodes));
            }
        }
    }
    
}
