package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
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
    
    public AdvertiseSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        
        this.subAddress = subAddress;
        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        
        this.baseId = state.getBaseId();
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
            // Sleep for a bit
            new SleepSubcoroutine.Builder()
                    .sourceAddress(subAddress)
                    .duration(Duration.ofSeconds(5L))
                    .timerAddress(timerAddress)
                    .build()
                    .run(cnt);
            
            
            List<Node> closestNodes = new FindSubcoroutine(subAddress.appendSuffix("find"), state, baseId, 20, true).run(cnt);
            applyNodesToRouter(ctx.getTime(), closestNodes);
        }
    }
    
    private void applyNodesToRouter(Instant time, List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getId().equals(baseId)) { // If we reached a node with our own id, skip it
                continue;
            }

            router.touch(time, node);
        }
    }
}
