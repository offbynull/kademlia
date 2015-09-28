package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
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

final class RefreshSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final AddressTransformer addressTransformer;
    private final IdGenerator idGenerator;
    private final SecureRandom secureRandom;
    
    private final Router router;
    private final Id baseId;
    
    public RefreshSubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        
        this.subAddress = subAddress;
        
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.addressTransformer = state.getAddressTransformer();
        this.idGenerator = state.getIdGenerator();
        this.secureRandom = state.getSecureRandom();
        
        this.baseId = state.getBaseId();
        this.router = state.getRouter();
        
    }
    
    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        int idBitLength = baseId.getBitLength();
        int idByteLength = idBitLength / 8 + (idBitLength % 8 == 0 ? 0 : 1);
        byte[] idBytes = new byte[idByteLength];
        
        while (true) {
            // Sleep for a bit
            new SleepSubcoroutine.Builder()
                    .sourceAddress(subAddress)
                    .duration(Duration.ofSeconds(5L))
                    .timerAddress(timerAddress)
                    .build()
                    .run(cnt);
            
            
            // Get closest nodes (from the router) to some random id
            secureRandom.nextBytes(idBytes);
            Id randomId = Id.create(idBytes, idBitLength);
            List<Node> nodesToPing = router.find(randomId, 5, true); // include stale nodes, because it may have come back up again / we may
                                                                     // have come back up again and we want to unstale it if we can contact
                                                                     // it
            
            
            // Ping them
            Address sourceAddress = subAddress.appendSuffix("refresh" + idGenerator.generate());
            MultiRequestSubcoroutine.Builder<PingResponse> multiReqBuilder = new MultiRequestSubcoroutine.Builder<PingResponse>()
                    .sourceAddress(sourceAddress)
                    .timerAddress(timerAddress)
                    .request(new PingRequest(baseId))
                    .addExpectedResponseType(PingResponse.class)
                    .attemptInterval(Duration.ofSeconds(2L))
                    .maxAttempts(5);
            
            Map<String, Node> suffixToNode = new HashMap<>();
            for (Node node : nodesToPing) {
                String link = node.getLink();
                Address dstAddress = addressTransformer.toAddress(link);
                
                String suffix = idGenerator.generate();
                suffixToNode.put(suffix, node);
                
                multiReqBuilder.addDestinationAddress(suffix, dstAddress);
            }
            
            List<Response<PingResponse>> responses = multiReqBuilder.build().run(cnt);
                    
            
            // Remove nodes that answered from suffixToNode
            for (Response<PingResponse> response : responses) {
                String suffix = response.getUniqueSourceAddressSuffix();
                suffixToNode.remove(suffix);
            }
            
            
            // Mark nodes that didn't answer as stale
            for (Node unresponsiveNode : suffixToNode.values()) {
                // DONT BOTHER WITH TRYING TO CALCULATE LOCKING/UNLOCKING LOGIC. THE LOGIC WILL BECOME EXTREMELY CONVOLUTED. THE QUERY
                // DID 5 REQUEST. IF NO ANSWER WAS GIVEN IN THE ALLOTED TIME, THEN MARK AS STALE!
                try {
                    router.stale(unresponsiveNode);
                } catch (NodeNotFoundException nnfe) { // may have been removed (already marked as stale) / may not be in routing tree
                    // Do nothing
                }
            }
        }
    }
}
