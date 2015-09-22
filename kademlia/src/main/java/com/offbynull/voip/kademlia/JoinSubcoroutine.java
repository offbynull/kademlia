package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.model.BitString;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class JoinSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final State state;
    
    private final Address logAddress;
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
        
        this.logAddress = state.getLogAddress();
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
        
        if (bootstrapNode == null) {
            ctx.addOutgoingMessage(subAddress, logAddress, info("Initial node in the network. No bootstrap to join."));
            return null;
        }
        
        List<Node> closestNodes;
        
        
        // 1. Add bootstrap node to routing tree
        ctx.addOutgoingMessage(subAddress, logAddress, info("Attempting to join the network via {}...", bootstrapNode));
        router.touch(ctx.getTime(), bootstrapNode);
        
        // 2. Find yourself
        ctx.addOutgoingMessage(subAddress, logAddress, info("Attempting to find self...", bootstrapNode));
        closestNodes = new FindSubcoroutine(subAddress.appendSuffix("selffind"), state, baseId, 20, false).run(cnt);
        Validate.validState(!closestNodes.isEmpty(), "No results from bootstrap");
        Validate.validState(!closestNodes.get(0).getId().equals(baseId), "Self already exists in network");
        applyNodesToRouter(ctx.getTime(), closestNodes);
        
        // There's a race condition here where 2 nodes with the same ID can be joining at the same time. There's no way to detect that case.
        
        // 3. Start bucket refreshes for all buckets
        ctx.addOutgoingMessage(subAddress, logAddress, info("Populating routing tree..."));
        List<BitString> bucketPrefixes = router.dumpBucketPrefixes();
        
        int idBitLength = baseId.getBitLength();
        int idByteLength = idBitLength / 8 + (idBitLength % 8 == 0 ? 0 : 1);
        byte[] idBytes = new byte[idByteLength];
        for (BitString bucketPrefix : bucketPrefixes) {
            // If the prefix is our own ID, don't try to find it
            if (baseId.getBitString().equals(bucketPrefix)) {
                continue;
            }
            
            // Create random id in bucket's range
            secureRandom.nextBytes(idBytes);
            Id randomId = Id.create(BitString.createLogicalOrder(idBytes, 0, idBitLength).setBits(0, bucketPrefix));
            
            ctx.addOutgoingMessage(subAddress, logAddress, info("Searching for random ID {}...", randomId));
            
            // Find closest nodes
            closestNodes = new FindSubcoroutine(subAddress.appendSuffix("bucketfind"), state, randomId, 20, false).run(cnt);
            
            // Touch router with these nodes
            applyNodesToRouter(ctx.getTime(), closestNodes);
        }
        
        // 4. Advertise self to closest nodes so people can reach you
        ctx.addOutgoingMessage(subAddress, logAddress, info("Finding closest nodes to self..."));
        closestNodes = new FindSubcoroutine(subAddress.appendSuffix("adv"), state, baseId, 20, true).run(cnt);
        applyNodesToRouter(ctx.getTime(), closestNodes);

        
        return null;
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
