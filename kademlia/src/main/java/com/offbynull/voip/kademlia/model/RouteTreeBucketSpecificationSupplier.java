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
package com.offbynull.voip.kademlia.model;

import org.apache.commons.lang3.Validate;

/**
 * K-bucket strategy for a Kademlia routing tree.
 * @author Kasra Faghihi
 */
public interface RouteTreeBucketSpecificationSupplier {

    /**
     * Get the k-bucket parameters for the k-bucket at some branch in the routing tree.
     * @param prefix prefix of branch where the newly created k-bucket will reside
     * @return k-bucket parameters
     * @throws NullPointerException if any argument is {@code null}
     */
    BucketParameters getBucketParameters(BitString prefix);

    /**
     * K-bucket parameters.
     */
    public static final class BucketParameters {
        private final int bucketSize;
        private final int cacheSize;

        /**
         * Construct a {@link BucketParameters} object.
         * @param bucketSize maximum number of nodes k-bucket can hold
         * @param cacheSize maximum number of cache nodes k-bucket can hold
         * @throws IllegalArgumentException if any numeric argument is negative
         */
        public BucketParameters(int bucketSize, int cacheSize) {
            Validate.isTrue(bucketSize >= 0);
            Validate.isTrue(cacheSize >= 0);
            this.bucketSize = bucketSize;
            this.cacheSize = cacheSize;
        }

        int getBucketSize() {
            return bucketSize;
        }

        int getCacheSize() {
            return cacheSize;
        }
    }
}
