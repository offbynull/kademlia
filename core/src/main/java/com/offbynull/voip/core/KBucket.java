package com.offbynull.voip.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.Validate;

public final class KBucket {

    private final Id baseId;

    private final BitString prefix;

    // the way thigns are done, cache and bucket should never contain the same node id at the same time
    private LeastRecentlySeenSet bucket;
    private MostRecentlySeenSet cache;

    private Instant lastUpdateTime;

    public KBucket(Id baseId, BitString prefix, int maxBucketSize, int maxCacheSize) {
        Validate.notNull(baseId);
        Validate.isTrue(prefix.getBitLength() < baseId.getBitLength()); // cannot == baseId.bitLength, because then you'd have an empty
                                                                        // bucket that you can't add anything to
        Validate.isTrue(maxBucketSize >= 0); // what's the point of a 0 size kbucket? let it thru anyways
        Validate.isTrue(maxCacheSize >= 0); // a cache size of 0 is not worthless...  may not care about having a replacement cache of nodes

        this.baseId = baseId;
        this.prefix = prefix;
        this.bucket = new LeastRecentlySeenSet(baseId, maxBucketSize);
        this.cache = new MostRecentlySeenSet(baseId, maxCacheSize);
        lastUpdateTime = Instant.MIN;
    }

    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(nodeId.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime

        
        LeastRecentlySeenSet.TouchResult bucketTouchRes = bucket.touch(time, node);
        switch (bucketTouchRes) {
            case UPDATED:
                lastUpdateTime = time;
                return TouchResult.UPDATED_BUCKET;
            case IGNORED:
                break; // bucket is full... break here so we can try adding it to cache
            case CONFLICTED:
                return TouchResult.DISCARDED;
            default:
                throw new IllegalStateException(); // should never happen
        }
        
        
        MostRecentlySeenSet.TouchResult cacheTouchRes = cache.touch(time, node);
        switch (cacheTouchRes) {
            case UPDATED:
                lastUpdateTime = time;
                return TouchResult.UPDATED_CACHE;
            case CONFLICTED:
                return TouchResult.DISCARDED;
            case IGNORED:
            default:
                throw new IllegalStateException(); // should never happen
        }
    }

    public ReplaceResult replace(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);

        if (cache.size() == 0) {
            return ReplaceResult.CACHE_EMPTY;
        }
        
        LeastRecentlySeenSet.RemoveResult removeRes = bucket.remove(node);
        switch (removeRes) {
            case CONFLICTED:
                return ReplaceResult.CONFLICTED;
            case NOT_FOUND:
                return ReplaceResult.NOT_FOUND;
            case REMOVED:
                Entry cacheEntry = cache.take();
                bucket.touch(cacheEntry.getLastSeenTime(), cacheEntry.getNode());
                return ReplaceResult.REPLACED;
            default:
                throw new IllegalStateException(); // should never happen
        }
    }
    
    public List<Entry> getClosest(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(max >= 0); // what's this point of calling this method if you want back 0 results??? let it thru anyways

        Validate.isTrue(!id.equals(baseId));
        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(id.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches
        
        List<Entry> nodes = bucket.dump();
        IdClosenessComparator comparator = new IdClosenessComparator(baseId);
        Collections.sort(nodes, (x, y) -> comparator.compare(x.getNode().getId(), y.getNode().getId()));
        
        int size = Math.min(max, nodes.size());
        
        return new ArrayList<>(nodes.subList(0, size));
    }

    // bitCount = 1 is 2 buckets
    // bitCount = 2 is 4 buckets
    // bitCount = 3 is 8 buckets
    public KBucket[] split(int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to split in to 2^30 buckets, but whatever
                                         // we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new Bucket[1 << 31] -- 1 << 31 is negative
                                         // new Bucket[1 << 30] -- 1 << 30 is positive
        
        Validate.isTrue(prefix.getBitLength() + bitCount <= baseId.getBitLength());

        // Create new buckets ...
        // Generates an array of buckets of 2^bitCount elements, where each bucket i has the current bucket's prefix with i appended to it.
        //
        // So for example, if this bucket's prefix = 1010 and bitCount = 2, the returning array would contain buckets with prefixes ...
        //
        // ret[0] = 1010 00   note that 0 = 00
        // ret[1] = 1010 01   note that 1 = 01
        // ret[2] = 1010 10   note that 2 = 10
        // ret[3] = 1010 11   note that 3 = 11
        //
        // note that bitCount = 2, 2^2 = 4, which results in 4 elements
        int maxBucketSize = bucket.getMaxSize();
        int maxCacheSize = cache.getMaxSize();
        int len = 1 << bitCount;
        KBucket[] newKBuckets = new KBucket[len];
        for (int i = 0; i < len; i++) {
            BitString numAsBitString = toBitString(i, bitCount);
            BitString appendedBitString = prefix.appendBits(numAsBitString);
            newKBuckets[i] = new KBucket(baseId, appendedBitString, maxBucketSize, maxCacheSize);
        }
        
        // Place entries in buckets
        for (Entry entry : bucket.dump()) {
            Node node = entry.getNode();
            
            // Read bitCount bits starting from prefixBitSize and use that to figure out which bucket to copy to
            // For example, if bitCount is 2 ...
            // If you read 00b, 00 = 0, so this ID will be go to newBucket[0]
            // If you read 01b, 01 = 1, so this ID will be go to newBucket[1]
            // If you read 10b, 10 = 2, so this ID will be go to newBucket[2]
            // If you read 11b, 11 = 3, so this ID will be go to newBucket[3]
            Id id = node.getId();
            int idx = (int) id.getBitsAsLong(prefix.getBitLength(), bitCount);
            
            TouchResult res;
            res = newKBuckets[idx].touch(entry.getLastSeenTime(), node); // send call to touch should set laset seen time
            Validate.validState(res == TouchResult.UPDATED_BUCKET); // should always happen, but just in case
        }
        
        return newKBuckets;
    }
    
    public void resizeBucket(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        int origMaxSize = bucket.getMaxSize();
        
        bucket.resize(maxSize);
        
        int unoccupiedBucketSlots = maxSize - origMaxSize;
        if (unoccupiedBucketSlots <= 0) {
            return;
        }

        for (int i = 0; i < unoccupiedBucketSlots; i++) {
            Entry cacheEntry = cache.take();
            if (cacheEntry == null) {
                break;
            }
            
            bucket.touch(cacheEntry.getLastSeenTime(), cacheEntry.getNode());
        }
    }
    
    public void resizeCache(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        cache.resize(maxSize);
    }

    public List<Entry> dumpBucket() {
        return bucket.dump();
    }

    public List<Entry> dumpCache() {
        return bucket.dump();
    }

    public int bucketSize() {
        return bucket.size();
    }
    
    public int cacheSize() {
        return cache.size();
    }
    
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    // The int {@code 0xABCD} with a bitlength of 12 would result in the bit string {@code 10 1011 1100 1101}.
    // Bit     15 14 13 12   11 10 09 08   07 06 05 04   03 02 01 00
    //         ------------------------------------------------------
    //         1  0  1  0    1  0  1  1    1  1  0  0    1  1  0  1
    //         A             B             C             D
    //               ^                                            ^
    //               |                                            | 
    //             start                                         end
    private static BitString toBitString(int data, int bitLength) {
        Validate.notNull(data);
        Validate.isTrue(bitLength > 0);

        data = data << (32 - bitLength);
        return BitString.createReadOrder(toBytes(data), 0, bitLength);
    }
    
    private static byte[] toBytes(int data) { // returns in big endian format
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            int shiftAmount = 24 - (i * 4);
            bytes[i] = (byte) (data >>> shiftAmount);
        }
        return bytes;
    }
    
    public enum TouchResult {

        UPDATED_BUCKET,
        UPDATED_CACHE,
        DISCARDED
    }
    
    public enum ReplaceResult {
        
        REPLACED, // removed from bucket and a node from cache replaced it
        NOT_FOUND, // node to be replaced was not found
        CACHE_EMPTY, // unable to remove from bucket because there's nothing in the cache to replace it with
        CONFLICTED, // entry with id exists in bucket, but link is different, so ignoring
    }
}
