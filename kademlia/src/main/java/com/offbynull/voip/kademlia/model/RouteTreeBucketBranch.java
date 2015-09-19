package com.offbynull.voip.kademlia.model;

import org.apache.commons.lang3.Validate;

public class RouteTreeBucketBranch implements RouteTreeBranch {

    private final KBucket kBucket;

    public RouteTreeBucketBranch(KBucket kBucket) {
        Validate.notNull(kBucket);
        this.kBucket = kBucket;
    }

    @Override
    public BitString getPrefix() {
        return kBucket.getPrefix();
    }

    @Override
    @SuppressWarnings("unchecked")
    public KBucket getItem() {
        return kBucket;
    }
}
