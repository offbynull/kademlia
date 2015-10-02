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
import com.offbynull.voip.kademlia.model.RouteTreeBranchStrategy;
import com.offbynull.voip.kademlia.model.RouteTreeBucketStrategy;
import com.offbynull.voip.kademlia.model.SimpleRouteTreeSpecificationSupplier;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

/**
 * Starts a Kademlia actor (priming message).
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
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
    
    /**
     * Constructs a {@link Start} object.
     * @param addressTransformer address transformer to use for converting node links to addresses (and vice-versa)
     * @param baseId ID to use for the node being started
     * @param bootstrapLink link to connect to the network (if {@code null} if means this node is the first node in the network)
     * @param kademliaParameters Kademlia parameters
     * @param seed1 random number generator seed value 1
     * @param seed2 random number generator seed value 2
     * @param timerAddress address of timer
     * @param graphAddress address of visualizer graph
     * @param logAddress address of logger
     * @throws NullPointerException if any argument other than {@code bootstrapLink} is {@code null}
     * @throws IllegalArgumentException if {@code seed1} or {@code seed2} is less than {@link IdGenerator#MIN_SEED_SIZE}
     */
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

    /**
     * Get address transformer used for converting node links to addresses (and vice-versa).
     * @return address transformer
     */
    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    /**
     * Get the ID to use for the node being started.
     * @return node ID
     */
    public Id getBaseId() {
        return baseId;
    }

    /**
     * Get the link to connect to the network.
     * @return link to connect to the network (if {@code null} if means this node is the first node in the network)
     */
    public String getBootstrapLink() {
        return bootstrapLink;
    }

    /**
     * Get the Kademlia parameters.
     * @return kademlia parameters
     */
    public KademliaParameters getKademliaParameters() {
        return kademliaParameters;
    }

    /**
     * Get the first random number generator seed value.
     * @return first seed value for RNG
     */
    public byte[] getSeed1() {
        return Arrays.copyOf(seed1, seed1.length);
    }

    /**
     * Get the second random number generator seed value.
     * @return second seed value for RNG
     */
    public byte[] getSeed2() {
        return Arrays.copyOf(seed2, seed2.length);
    }

    /**
     * Get the timer address.
     * @return timer address
     */
    public Address getTimerAddress() {
        return timerAddress;
    }

    /**
     * Get the visualizer graph address.
     * @return visualizer graph address
     */
    public Address getGraphAddress() {
        return graphAddress;
    }

    /**
     * Get the logger address.
     * @return logger address
     */
    public Address getLogAddress() {
        return logAddress;
    }
    
    /**
     * Kademlia parameters.
     */
    public static final class KademliaParameters {
        private final Supplier<RouteTreeBranchStrategy> branchStrategySupplier;
        private final Supplier<RouteTreeBucketStrategy> bucketStrategySupplier;
        private final int maxConcurrentRequestsPerFind;


        /**
         * Constructs a {@link KademliaParameters} object.
         * @param baseId ID of the Kademlia node this parameter is for
         * @param branchesPerLevel number of branches in the routing tree at each depth (must be power of 2 and greater than 1)
         * @param nodesPerBucket maximum number of nodes allowed in each k-bucket
         * @param cacheNodesPerBucket maximum number of nodes allowed in each k-bucket's cache
         * @param maxConcurrentRequestsPerFind maximum number of requests to run concurrently for each search
         * @throws NullPointerException if any argument is {@code null}
         * @throws IllegalArgumentException if any numeric argument is {@code < 0}, or if {@code maxConcurrentRequestsPerFind <= 0}, or if
         * {@code branchesPerLevel <= 1 || !isPowerOfTwo(branchesPerLevel)}
         */
        public KademliaParameters(Id baseId, int branchesPerLevel, int nodesPerBucket, int cacheNodesPerBucket,
                int maxConcurrentRequestsPerFind) {
            this(() -> new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, nodesPerBucket, cacheNodesPerBucket),
                    () -> new SimpleRouteTreeSpecificationSupplier(baseId, branchesPerLevel, nodesPerBucket, cacheNodesPerBucket),
                    maxConcurrentRequestsPerFind);
        }

        /**
         * Constructs a {@link KademliaParameters} object.
         * @param branchStrategySupplier branching strategy to use when building routing tree (make sure the {@link Supplier} passed
         * in here generates a new {@link RouteTreeBranchStrategy} every time {@link Supplier#get() } is invoked)
         * @param bucketStrategySupplier bucket creation strategy to use when building k-bucket within routing tree (make sure the
         * {@link Supplier} passed in here generates a new {@link RouteTreeBucketStrategy} every time {@link Supplier#get() }
         * is invoked)
         * @param maxConcurrentRequestsPerFind maximum number of requests to run concurrently for each search
         * @throws NullPointerException if any argument is {@code null}
         * @throws IllegalArgumentException if {@code maxConcurrentRequestsPerFind <= 0}
         */
        public KademliaParameters(Supplier<RouteTreeBranchStrategy> branchStrategySupplier,
                Supplier<RouteTreeBucketStrategy> bucketStrategySupplier,
                int maxConcurrentRequestsPerFind) {
            Validate.notNull(branchStrategySupplier);
            Validate.notNull(bucketStrategySupplier);
            Validate.notNull(maxConcurrentRequestsPerFind > 0);
            this.branchStrategySupplier = branchStrategySupplier;
            this.bucketStrategySupplier = bucketStrategySupplier;
            this.maxConcurrentRequestsPerFind = maxConcurrentRequestsPerFind;
        }

        /**
         * Get the supplier that generates a new branching strategy.
         * @return supplier that generates a new branching strategy
         */
        public Supplier<RouteTreeBranchStrategy> getBranchStrategy() {
            return branchStrategySupplier;
        }

        /**
         * Get the supplier that generates a new bucket strategy.
         * @return supplier that generates a new bucket strategy
         */
        public Supplier<RouteTreeBucketStrategy> getBucketStrategy() {
            return bucketStrategySupplier;
        }

        /**
         * Get the maximum number of requests to run concurrently for each search.
         * @return max number of requests allowed at one time for each search
         */
        public int getMaxConcurrentRequestsPerFind() {
            return maxConcurrentRequestsPerFind;
        }
        
    }
}
