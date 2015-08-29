package com.offbynull.voip.core;

import java.time.Instant;
import java.util.Comparator;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

public final class KBucket {
    private final Id id;
    private final int commonPrefixSize; // For something to be allowed in this bucket, it needs to share a common prefix of this many bits
                                        // with id field

    private final int bucketSize; // k -- k is a system-wide replication parameter. k is chosen such that any given k nodes are very
                                  // unlikely to fail within an hour of eachother (for example k = 20)
    
    private final TreeSet<KBucketEntry> nodes; // Each k-bucket is kept sorted by time last seen
    
    private Instant lastUpdateTime;

    public KBucket(Id id, int commonPrefixSize, int bucketSize) {
        Validate.notNull(id);
        Validate.isTrue(commonPrefixSize >= 0);
        Validate.isTrue(commonPrefixSize <= id.getBitLength());
        Validate.isTrue(bucketSize > 0);
        
        this.id = id;
        this.commonPrefixSize = commonPrefixSize;
        this.bucketSize = bucketSize;

        this.nodes = new TreeSet<>(new KBucketEntryLastSeenTimeComparator());
        
        lastUpdateTime = Instant.MIN;
    }
  
//    public UpdateResult touch(Instant time, NodeInformation nodeInfo) {
//        Validate.notNull(time);
//        Validate.notNull(nodeInfo);
//        Validate.isTrue(time.isAfter(lastUpdateTime));
//        
//        
//        
//        lastUpdateTime = time;
//    }
    
// TODO: Is it worth writing a splitting method?

//    // bitCount = 1 is 2 buckets
//    // bitCount = 2 is 4 buckets
//    // bitCount = 3 is 8 buckets
//    public KBucket[] split(int bitCount) {
//    }
//
//    public UpdateResult update(Instant time, NodeInformation nodeInfo) {
//        Validate.notNull(time);
//        Validate.notNull(nodeInfo);
//        Validate.isTrue(time.isAfter(lastUpdateTime));
//
//
//
//        lastUpdateTime = time;
//    }
//
//    public enum UpdateResult {
//        INSERTED, // inserted as latest entry
//        FULL // latest entry needs to be pinged to see if its still alive, if it isn't remove then update again to add this guy back in
//    }
//
}
