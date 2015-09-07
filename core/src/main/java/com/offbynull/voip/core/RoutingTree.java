package com.offbynull.voip.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class RoutingTree {
    private final Id baseId;
    private final ArrayList<KBucket> buckets;

    public RoutingTree(Id baseId, int depth, int bucketSize) {
        Validate.notNull(baseId);
        
        int maxDepth = baseId.getBitLength(); // depth can't == baseId.bitLength, because then you'll have a bucket with only you as the
                                              // allowed element
        
        Validate.isTrue(maxDepth > 0);
        Validate.isTrue(maxDepth < depth);
        
        this.baseId = baseId;
        buckets = new ArrayList<>(depth);
        
        for (int i = 0; i < depth; i++) {
            BitString prefix = baseId.getBitString().getBits(0, i);
            KBucket bucket = new KBucket(baseId, prefix, maxDepth, bucketSize);
            buckets.add(bucket);
        }
    }
    
    public void touch(Instant time, Node node) {
        
    }
    
    public void unresponsive(Instant time, Node node) {
        
    }
    
    public enum TouchResult {
        TOUCHED_BUCKET,
        TOUCHED_CACHE,
        IGNORED
    }

    public enum UnresponsiveResult {
        CLEARED_BUCKET,
        CLEARED_CACHE,
        IGNORED
    }
    
    private static final class DepthParameters {
        private final BucketParameters[] bucketParams;
        
        public DepthParameters(BucketParameters[] bucketParams) {
            Validate.notNull(bucketParams);
            Validate.noNullElements(bucketParams);
            this.bucketParams = Arrays.copyOf(bucketParams, bucketParams.length);
        }
    }
    
    private static final class BucketParameters {
        private final int bucketSize;
        private final int cacheSize;

        public BucketParameters(int bucketSize, int cacheSize) {
            Validate.isTrue(bucketSize >= 0);
            Validate.isTrue(cacheSize >= 0);
            this.bucketSize = bucketSize;
            this.cacheSize = cacheSize;
        }

        public int getBucketSize() {
            return bucketSize;
        }

        public int getCacheSize() {
            return cacheSize;
        }
    }
}
