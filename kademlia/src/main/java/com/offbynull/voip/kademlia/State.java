/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.offbynull.voip.kademlia;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.voip.kademlia.model.Router;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class State {

    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    
    private final IdGenerator idGenerator;
    private final Router router;
    
    private final AddressTransformer addressTransformer;

    public State(
            Address timerAddress,
            Address graphAddress,
            Address logAddress,
            byte[] seed,
            Router router,
            AddressTransformer addressTransformer) {
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.notNull(logAddress);
        Validate.notNull(seed);
        Validate.notNull(router);
        Validate.notNull(addressTransformer);
        Validate.isTrue(seed.length >= IdGenerator.MIN_SEED_SIZE);
        this.timerAddress = timerAddress;
        this.graphAddress = graphAddress;
        this.logAddress = logAddress;
        idGenerator = new IdGenerator(seed);
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

    public Router getRouter() {
        return router;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }
}