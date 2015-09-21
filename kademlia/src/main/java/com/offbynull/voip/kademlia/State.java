/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.offbynull.voip.kademlia;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.Router;
import org.apache.commons.lang3.Validate;

final class State {

    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    private final IdGenerator idGenerator;
    
    private final Id baseId;
    private final Router router;
    
    private final AddressTransformer addressTransformer;

    public State(
            Address timerAddress,
            Address graphAddress,
            Address logAddress,
            byte[] seed,
            Id baseId,
            Router router,
            AddressTransformer addressTransformer) {
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        Validate.notNull(seed);
        Validate.notNull(baseId);
        Validate.notNull(router);
        Validate.notNull(addressTransformer);
        Validate.isTrue(seed.length >= IdGenerator.MIN_SEED_SIZE);
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
        idGenerator = new IdGenerator(seed);
        this.baseId = baseId;
        this.router = router;
        this.addressTransformer = addressTransformer;
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

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public Id getBaseId() {
        return baseId;
    }

    public Router getRouter() {
        return router;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }
}