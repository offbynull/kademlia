/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import com.offbynull.peernetic.core.actor.helpers.IdGenerator;
import com.offbynull.voip.kademlia.model.Id;
import com.offbynull.voip.kademlia.model.RouteTreeBranchSpecificationSupplier;
import com.offbynull.voip.kademlia.model.RouteTreeBucketSpecificationSupplier;
import com.offbynull.voip.kademlia.model.SimpleRouteTreeSpecificationSupplier;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

public final class Start {

    private final AddressTransformer addressTransformer;
    private final Id baseId;
    private final String bootstrapLink;
    private final byte[] seed1;
    private final byte[] seed2;
    private final Address timerAddress;
    private final Address graphAddress;
    private final Address logAddress;
    private final KademliaParameters kademliaParameters;
    
    public Start(
            AddressTransformer addressTransformer,
            Id baseId,
            String bootstrapLink,
            KademliaParameters kademliaParameters,
            byte[] seed1,
            byte[] seed2,
            Address timerAddress,
            Address graphAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        Validate.notNull(baseId);
        // bootstrapNode can be null
        Validate.notNull(kademliaParameters);
        Validate.notNull(seed1);
        Validate.notNull(seed2);
        Validate.notNull(timerAddress);
        Validate.notNull(graphAddress);
        Validate.isTrue(seed1.length >= IdGenerator.MIN_SEED_SIZE);
        Validate.isTrue(seed2.length >= IdGenerator.MIN_SEED_SIZE);
        this.addressTransformer = addressTransformer;
        this.baseId = baseId;
        this.bootstrapLink = bootstrapLink;
        this.kademliaParameters = kademliaParameters;
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

    public String getBootstrapLink() {
        return bootstrapLink;
    }

    public KademliaParameters getKademliaParameters() {
        return kademliaParameters;
    }

    public byte[] getSeed1() {
        return Arrays.copyOf(seed1, seed1.length);
    }

    public byte[] getSeed2() {
        return Arrays.copyOf(seed2, seed2.length);
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

    
    public static final class KademliaParameters {
        private final Supplier<RouteTreeBranchSpecificationSupplier> branchSpecSupplier;
        private final Supplier<RouteTreeBucketSpecificationSupplier> bucketSpecSupplier;
        private final int maxConcurrentRequestsPerFind;


        public KademliaParameters(Id baseId, int branchesPerLevel, int nodesPerBucket, int cacheNodesPerBucket,
                int maxConcurrentRequestsPerFind) {
            this(() -> new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, nodesPerBucket, cacheNodesPerBucket),
                    () -> new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, nodesPerBucket, cacheNodesPerBucket),
                    maxConcurrentRequestsPerFind);
        }

        public KademliaParameters(Supplier<RouteTreeBranchSpecificationSupplier> branchSpecSupplier,
                Supplier<RouteTreeBucketSpecificationSupplier> bucketSpecSupplier,
                int maxConcurrentRequestsPerFind) {
            Validate.notNull(branchSpecSupplier);
            Validate.notNull(bucketSpecSupplier);
            Validate.notNull(maxConcurrentRequestsPerFind > 0);
            this.branchSpecSupplier = branchSpecSupplier;
            this.bucketSpecSupplier = bucketSpecSupplier;
            this.maxConcurrentRequestsPerFind = maxConcurrentRequestsPerFind;
        }

        public Supplier<RouteTreeBranchSpecificationSupplier> getBranchSpecSupplier() {
            return branchSpecSupplier;
        }

        public Supplier<RouteTreeBucketSpecificationSupplier> getBucketSpecSupplier() {
            return bucketSpecSupplier;
        }

        public int getMaxConcurrentRequestsPerFind() {
            return maxConcurrentRequestsPerFind;
        }
        
    }
}
