package com.offbynull.voip.kademlia;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    
    public List<Entry> getClosest(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(max >= 0); // what's this point of calling this method if you want back 0 results??? let it thru anyways

        Validate.isTrue(!id.equals(baseId));
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches
        
        List<Entry> nodes = bucket.dump();
        IdClosenessComparator comparator = new IdClosenessComparator(id);
        Collections.sort(nodes, (x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()));
        
        int size = Math.min(max, nodes.size());
        
        return new ArrayList<>(nodes.subList(0, size));
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
