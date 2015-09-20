package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.CoroutineActor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.externalmessages.FindResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.IdClosenessComparator;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public final class FindSubcoroutine implements Subcoroutine<List<Node>> {

    private final Address subAddress;
    private final Address graphAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final AddressTransformer addressTransformer;
    private final IdGenerator idGenerator;
    private final Router router;
    
    private final Id baseId;
    private final Id findId;
    private final int maxResults;
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public List<Node> run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        IdClosenessComparator idClosenessComparator = new IdClosenessComparator(findId);
        
        List<Node> res = router.find(findId, maxResults);
        while (true) {
            MultiRequestSubcoroutine.Builder<FindResponse> multiReqSubBuilder = new MultiRequestSubcoroutine.Builder<>();
            
            for (Node node : res) {
                String link = node.getLink();
                Address address = addressTransformer.linkIdToRemoteAddress(link);
                multiReqSubBuilder.addDestinationAddress(idGenerator, address);
            }
            
            MultiRequestSubcoroutine<FindResponse> multiReqSub = multiReqSubBuilder
                    .attemptInterval(Duration.ofMillis(5L))
                    .maxAttempts(5)
                    .addExpectedResponseType(FindResponse.class)
                    .sourceAddress(Address.of(idGenerator.generate()))
                    .timerAddress(timerAddress)
                    .build();
            
            List<Response<FindResponse>> responses = multiReqSub.run(cnt);
            
            TreeSet<Node> sortedNodeSet = new TreeSet<>((x, y) -> idClosenessComparator.compare(x.getId(), y.getId()));
            for (Response<FindResponse> response : responses) {
                FindResponse findResponse = response.getResponse();
                Node[] nodes = findResponse.getNodes();
                
                sortedNodeSet.addAll(Arrays.asList(nodes));
            }
            
            TRUNCATE sortedNodeSet HERE;
        }
    }
    
}
