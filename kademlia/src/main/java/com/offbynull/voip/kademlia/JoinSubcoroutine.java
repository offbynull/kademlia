package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.voip.kademlia.AddressConstants.ROUTER_EXT_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.voip.kademlia.externalmessages.PingRequest;
import com.offbynull.voip.kademlia.externalmessages.PingResponse;
import com.offbynull.voip.kademlia.model.BitString;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import com.offbynull.voip.kademlia.model.Router;
import com.offbynull.voip.kademlia.model.RouterChangeSet;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class JoinSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address logAddress;
    private final Address timerAddress;
    
    private final AddressTransformer addressTransformer;
    private final IdGenerator idGenerator;
    private final SecureRandom secureRandom;
    
    private final State state;
    
    private final String bootstrapLink;
    private final Router router;
    private final Id baseId;
    
    private final GraphHelper graphHelper;
    
    public JoinSubcoroutine(Address subAddress, State state, String bootstrapLink) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        
        this.subAddress = subAddress;
        this.logAddress = state.getLogAddress();
        this.timerAddress = state.getTimerAddress();
        
        this.addressTransformer = state.getAddressTransformer();
        this.idGenerator = state.getIdGenerator();
        this.secureRandom = state.getSecureRandom();
        
        this.state = state;
        
        this.bootstrapLink = bootstrapLink;
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
        
        if (bootstrapLink == null) {
            ctx.addOutgoingMessage(subAddress, logAddress, info("Initial node in the network. No bootstrap to join."));
            return null;
        }
        
        // 0. Get ID of bootstarp node
        ctx.addOutgoingMessage(subAddress, logAddress, info("Getting ID for bootstrap link {}", bootstrapLink));
        Address destinationAddress = addressTransformer.toAddress(bootstrapLink)
                .appendSuffix(ROUTER_EXT_HANDLER_RELATIVE_ADDRESS);
        PingResponse pingResponse = new RequestSubcoroutine.Builder<PingResponse>()
                .sourceAddress(subAddress, idGenerator)
                .destinationAddress(destinationAddress)
                .timerAddress(timerAddress)
                .request(new PingRequest(null))
                .addExpectedResponseType(PingResponse.class)
                .attemptInterval(Duration.ofSeconds(2L))
                .maxAttempts(5)
                .throwExceptionIfNoResponse(false)
                .build()
                .run(cnt);

        Validate.isTrue(pingResponse != null, "Bootstrap link {} did not respond to ping", bootstrapLink);
        Id bootstrapId = pingResponse.getId();
        Validate.isTrue(!bootstrapId.equals(baseId), "Bootstrap ID {} conflicts with self", bootstrapId); // bootstrap must not be self
        
        Node bootstrapNode = new Node(pingResponse.getId(), bootstrapLink);
        
        // 1. Add bootstrap node to routing tree
        ctx.addOutgoingMessage(subAddress, logAddress, info("Attempting to join the network via {}...", bootstrapNode));
        applyNodesToRouter(ctx, Collections.singletonList(bootstrapNode));
        
        // 2. Find yourself
        List<Node> closestNodes;
        
        ctx.addOutgoingMessage(subAddress, logAddress, info("Attempting to find self...", bootstrapNode));
        closestNodes = new FindSubcoroutine(subAddress.appendSuffix("selffind"), state, baseId, 20, false, false).run(cnt);
        Validate.validState(!closestNodes.isEmpty(), "No results from bootstrap");
        Validate.validState(!closestNodes.get(0).getId().equals(baseId), "Self already exists in network");
        applyNodesToRouter(ctx, closestNodes);
        
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
            closestNodes = new FindSubcoroutine(subAddress.appendSuffix("bucketfind"), state, randomId, 20, false, false).run(cnt);
            
            // Touch router with these nodes
            applyNodesToRouter(ctx, closestNodes);
        }
        
        // 4. Advertise self to closest nodes so people can reach you
        ctx.addOutgoingMessage(subAddress, logAddress, info("Finding closest nodes to self..."));
        closestNodes = new FindSubcoroutine(subAddress.appendSuffix("adv"), state, baseId, 20, true, true).run(cnt);
        applyNodesToRouter(ctx, closestNodes);

        
        return null;
    }

    private void applyNodesToRouter(Context ctx, List<Node> nodes) {
        for (Node node : nodes) {
            if (node.getId().equals(baseId)) { // If we reached a node with our own id, skip it
                continue;
            }

            RouterChangeSet changeSet = router.touch(ctx.getTime(), node);
            graphHelper.applyRouterChanges(ctx, changeSet);
        }
    }
}
