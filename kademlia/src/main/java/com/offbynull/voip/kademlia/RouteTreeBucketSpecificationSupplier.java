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
package com.offbynull.voip.kademlia;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;


public interface RouteTreeBucketSpecificationSupplier {

    BucketParameters getBucketParameters(BitString prefix);

    public static final class BucketParameters {
        private final int bucketSize;
        private final int cacheSize;

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