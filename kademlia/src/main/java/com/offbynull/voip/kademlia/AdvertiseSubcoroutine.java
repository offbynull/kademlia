package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.externalmessages.PingRequest;
import com.offbynull.voip.kademlia.externalmessages.PingResponse;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.NodeNotFoundException;
import com.offbynull.voip.kademlia.model.Router;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class AdvertiseSubcoroutine implements Subcoroutine<Void> {

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
        while (true) {
            // Sleep for a bit
            new SleepSubcoroutine.Builder()
                    .sourceAddress(subAddress)
                    .duration(Duration.ofSeconds(5L))
                    .timerAddress(timerAddress)
                    .build()
                    .run(cnt);
            
            
            new FindSubcoroutine(subAddress.appendSuffix("adv"), state, baseId, 20, true).run(cnt);
            // should we do anything with these results? just by virtue of "finding", we would have hit the closest nodes
        }
    }
}
