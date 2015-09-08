package com.offbynull.voip.core;

import org.apache.commons.lang3.Validate;

public final class KBucketChangeSet {
    private final ChangeSet bucketChangeSet;
    private final ChangeSet cacheChangeSet;

    public KBucketChangeSet(ChangeSet bucketChangeSet, ChangeSet cacheChangeSet) {
        Validate.notNull(bucketChangeSet);
        Validate.notNull(cacheChangeSet);
        this.bucketChangeSet = bucketChangeSet;
        this.cacheChangeSet = cacheChangeSet;
    }

    public ChangeSet getBucketChangeSet() {
        return bucketChangeSet;
    }

    public ChangeSet getCacheChangeSet() {
        return cacheChangeSet;
    }
    
}
