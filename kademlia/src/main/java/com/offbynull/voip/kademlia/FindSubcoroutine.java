package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import java.util.List;

public final class FindSubcoroutine implements Subcoroutine<List<Node>> {

    private final Address subAddress;
    private final Address graphAddress;
    private final Address timerAddress;
    private final Address logAddress;
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
        List<Node> res = router.find(findId, maxResults);
        
        
    }
    
}
