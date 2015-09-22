package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.model.BitString;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class JoinSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;
    
    private final Address timerAddress;
    private final Address logAddress;
    private final AddressTransformer addressTransformer;
    private final IdGenerator idGenerator;
    private final SecureRandom secureRandom;
    
    private final Node bootstrapNode;
    private final Router router;
    private final Id baseId;
    
    public JoinSubcoroutine(Address subAddress, State state, Node bootstrapNode) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        Validate.notNull(bootstrapNode);
        
        this.subAddress = subAddress;
        this.state = state;
        
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.addressTransformer = state.getAddressTransformer();
        this.idGenerator = state.getIdGenerator();
        this.secureRandom = state.getSecureRandom();
        
        this.bootstrapNode = bootstrapNode;
        this.baseId = state.getBaseId();
        this.router = state.getRouter();
        
        Validate.isTrue(!bootstrapNode.getId().equals(baseId)); // bootstrap must not be self
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        // 1. Add bootstrap node to routing tree
        router.touch(ctx.getTime(), bootstrapNode);
        
        // 2. Find yourself
        List<Node> nodesClosestToSelf = new FindSubcoroutine(subAddress.appendSuffix("selffind"), state, baseId, 20).run(cnt);
        Validate.validState(!nodesClosestToSelf.isEmpty(), "No results from bootstrap");
        Validate.validState(!nodesClosestToSelf.get(0).getId().equals(baseId), "Self already exists in network");
        
        // 3. Start bucket refreshes for all buckets
        List<BitString> bucketPrefixes = router.dumpBucketPrefixes();
        Collections.sort(bucketPrefixes, (x, y) -> Integer.compare(x.getBitLength(), y.getBitLength())); // sort by bitlength size
        
        int idBitLength = baseId.getBitLength();
        int idByteLength = idBitLength / 8 + (idBitLength % 8 == 0 ? 0 : 1);
        byte[] idBytes = new byte[idByteLength];
        for (BitString bucketPrefix : bucketPrefixes) {
            // Create random id in bucket's range
            secureRandom.nextBytes(idBytes);
            Id randomId = Id.create(BitString.createLogicalOrder(idBytes, 0, idBitLength).setBits(0, bucketPrefix));
            
            // Find closest nodes
            List<Node> nodesClosestToRandomId = new FindSubcoroutine(subAddress.appendSuffix("bucketfind"), state, randomId, 20).run(cnt);
            
            // Touch router with these nodes
            for (Node node : nodesClosestToRandomId) {
                
            }
        }
        
        // 4. Advertise self to closest nodes so people can reach you
        
        return null;
    }
    
}
