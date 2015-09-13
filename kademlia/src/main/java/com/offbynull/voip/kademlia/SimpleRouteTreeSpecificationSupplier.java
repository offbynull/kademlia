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

public final class SimpleRouteTreeSpecificationSupplier implements RouteTreeBranchSpecificationSupplier,
        RouteTreeBucketSpecificationSupplier {
    private final Id baseId;
    private final int branchesPerLevel;
    private final int nodesPerBucket;
    private final int cacheNodesPerBucket;

    public SimpleRouteTreeSpecificationSupplier(Id baseId, int branchesPerLevel, int nodesPerBucket, int cacheNodesPerBucket) {
        Validate.notNull(baseId);
        Validate.isTrue(branchesPerLevel > 0);
        Validate.isTrue(nodesPerBucket > 0);
        Validate.isTrue(cacheNodesPerBucket > 0);
        
        // check to make sure power of 2
        // other ways: http://javarevisited.blogspot.ca/2013/05/how-to-check-if-integer-number-is-power-of-two-example.html
        Validate.isTrue(Integer.bitCount(branchesPerLevel) == 1);
        
        this.baseId = baseId;
        this.branchesPerLevel = branchesPerLevel;
        this.nodesPerBucket = nodesPerBucket;
        this.cacheNodesPerBucket = cacheNodesPerBucket;
    }

    @Override
    public int getBranchCount(BitString prefix) {
        Validate.notNull(prefix);
        
        if (prefix.getBitLength() >= baseId.getBitLength()) {
            // Maximum tree depth reached, cannot branch any further
            return 0;
        }
        
        return branchesPerLevel;
    }

    @Override
    public BucketParameters getBucketParameters(BitString prefix) {
        Validate.notNull(prefix);
        return new BucketParameters(nodesPerBucket, cacheNodesPerBucket);
    }
    
}
