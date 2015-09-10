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

import org.apache.commons.lang3.Validate;

public final class SimpleRouteTreeSpecificationSupplier implements RouteTreeSpecificationSupplier {
    private final Id baseId;
    private final int bucketsPerLevel;
    private final int nodesPerBucket;
    private final int cacheNodesPerBucket;

    public SimpleRouteTreeSpecificationSupplier(Id baseId, int bucketsPerLevel, int nodesPerBucket, int cacheNodesPerBucket) {
        Validate.notNull(baseId);
        Validate.isTrue(bucketsPerLevel > 0);
        Validate.isTrue(nodesPerBucket > 0);
        Validate.isTrue(cacheNodesPerBucket > 0);
        
        // check to make sure power of 2
        // other ways: http://javarevisited.blogspot.ca/2013/05/how-to-check-if-integer-number-is-power-of-two-example.html
        Validate.isTrue(Integer.bitCount(bucketsPerLevel) == 1);
        
        this.baseId = baseId;
        this.bucketsPerLevel = bucketsPerLevel;
        this.nodesPerBucket = nodesPerBucket;
        this.cacheNodesPerBucket = cacheNodesPerBucket;
    }

    @Override
    public DepthParameters getParameters(int prefixLen) {
        Validate.isTrue(prefixLen >= 0);
        Validate.isTrue(prefixLen < baseId.getBitLength());
        
        BucketParameters[] bucketParams = new BucketParameters[bucketsPerLevel];
        for (int i = 0; i < bucketsPerLevel; i++) {
            bucketParams[i] = new BucketParameters(nodesPerBucket, cacheNodesPerBucket);
        }
        return new DepthParameters(bucketParams);
    }
    
}
