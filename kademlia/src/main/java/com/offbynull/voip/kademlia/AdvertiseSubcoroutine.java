package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import com.offbynull.voip.kademlia.model.RouterChangeSet;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class AdvertiseSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;
    
    private final Address timerAddress;
    
    private final Router router;
    private final Id baseId;
    
    private final GraphHelper graphHelper;
    
    public AdvertiseSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        
        this.subAddress = subAddress;
        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        
        this.baseId = state.getBaseId();
        this.router = state.getRouter();
        
        this.graphHelper = state.getGraphHelper();
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        while (true) {
            // Sleep for a bit
            new SleepSubcoroutine.Builder()
                    .sourceAddress(subAddress)
                    .duration(Duration.ofSeconds(5L))
                    .timerAddress(timerAddress)
                    .build()
                    .run(cnt);
            
            
            List<Node> closestNodes = new FindSubcoroutine(subAddress.appendSuffix("find"), state, baseId, 20, true, true).run(cnt);
            for (Node node : closestNodes) {
                RouterChangeSet changeSet = router.touch(ctx.getTime(), node); // since we set ignoreSelf to true, node will never == baseId
                graphHelper.applyRouterChanges(ctx, changeSet);
            }
        }
    }
}
