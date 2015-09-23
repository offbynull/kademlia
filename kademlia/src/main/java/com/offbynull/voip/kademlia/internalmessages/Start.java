package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Node;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final Id baseId;
    private final Node bootstrapNode;
    private final byte[] seed1;
    private final byte[] seed2;
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    public Start(
            AddressTransformer addressTransformer,
            Id baseId,
            Node bootstrapNode,
            byte[] seed1,
            byte[] seed2,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        Validate.notNull(baseId);
        // bootstrapNode can be null
        Validate.notNull(seed1);
        Validate.notNull(seed2);
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.isTrue(seed1.length >= IdGenerator.MIN_SEED_SIZE);
        Validate.isTrue(seed2.length >= IdGenerator.MIN_SEED_SIZE);
        this.addressTransformer = addressTransformer;
        this.baseId = baseId;
        this.bootstrapNode = bootstrapNode;
        this.seed1 = Arrays.copyOf(seed1, seed1.length);
        this.seed2 = Arrays.copyOf(seed2, seed2.length);
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public Id getBaseId() {
        return baseId;
    }

    public Node getBootstrapNode() {
        return bootstrapNode;
    }

    public byte[] getSeed1() {
        return seed1;
    }

    public byte[] getSeed2() {
        return seed2;
    }

    public Address getTimerAddress() {
        return timerAddress;
    }

    public Address getGraphAddress() {
        return graphAddress;
    }

    public Address getLogAddress() {
        return logAddress;
    }

}
