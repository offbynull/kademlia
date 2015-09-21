package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.externalmessages.FindResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.IdClosenessComparator;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
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
    
    private final Id findId;
    private final int maxResults;
    
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
        
        IdClosenessComparator idClosenessComparator = new IdClosenessComparator(findId);
        
        // Get initial set of nodes to query from routing table
        List<Node> queryNodes = router.find(findId, maxResults);
        ctx.addOutgoingMessage(subAddress, logAddress, info("Initial route table entries closest to {}: {}", findId, queryNodes));
        
        while (true) {
            // If you've found the ID you're looking for, return immediately
            Node closestQueriedNode = queryNodes.get(0);
            if (closestQueriedNode.getId().equals(findId)) {
                return queryNodes;
            }
            
            
            // Build multirequest to nodes in queryNodes
            MultiRequestSubcoroutine.Builder<FindResponse> multiReqSubBuilder = new MultiRequestSubcoroutine.Builder<>();
            
            Map<String, Node> uniqueSuffixToQueryNode = new HashMap<>();
            for (Node queryNode : queryNodes) {
                String link = queryNode.getLink();
                Address address = addressTransformer.linkIdToRemoteAddress(link);
                
                String uniqueRequestSourceSuffix = idGenerator.generate();
                uniqueSuffixToQueryNode.put(uniqueRequestSourceSuffix, queryNode);
                
                multiReqSubBuilder.addDestinationAddress(uniqueRequestSourceSuffix, address);
            }
            
            MultiRequestSubcoroutine<FindResponse> multiReqSub = multiReqSubBuilder
                    .attemptInterval(Duration.ofMillis(2L))
                    .maxAttempts(5)
                    .addExpectedResponseType(FindResponse.class)
                    .sourceAddress(subAddress.appendSuffix(idGenerator.generate()))
                    .timerAddress(timerAddress)
                    .build();
            
            
            // Execute multirequest
            List<Response<FindResponse>> responses = multiReqSub.run(cnt);
            
            
            // Process responses from multirequest
            Set<Node> nonResponsiveNodes = new HashSet<>(uniqueSuffixToQueryNode.values());
            TreeSet<Node> sortedResponses = new TreeSet<>((x, y) -> idClosenessComparator.compare(x.getId(), y.getId()));
            for (Response<FindResponse> response : responses) {
                // Remove nodes that responded from nonResponsiveNodes
                String reqSuffix = response.getUniqueSourceAddressSuffix();
                Node queriedNode = uniqueSuffixToQueryNode.get(reqSuffix);
                nonResponsiveNodes.remove(queriedNode);
                
                // Get response
                FindResponse findResponse = response.getResponse();
                Node[] nodes = findResponse.getNodes();
                
                // Touch responding node
                router.touch(ctx.getTime(), queriedNode);
                
                // Add node results to sorted set
                sortedResponses.addAll(Arrays.asList(nodes));
                
                // Make sure sortedNodeSet doesn't exceed past maxResults -- remove the ones that are farthest away
                while (sortedResponses.size() > maxResults) {
                    sortedResponses.pollLast();
                }
            }
            
            
            // Lock nodes that didn't respond
            for (Node node : nonResponsiveNodes) {
                
            }
            
            
            // Get only the nodes that are closer than the closest node you queried, if there are non, then return the set of nodes you
            // queried
            SortedSet<Node> closerThanQueriedNodes = sortedResponses.headSet(closestQueriedNode, false);
            if (closerThanQueriedNodes.isEmpty()) {
                return queryNodes;
            }
            
            queryNodes = new ArrayList<>(closerThanQueriedNodes);
        }
    }
    
}
