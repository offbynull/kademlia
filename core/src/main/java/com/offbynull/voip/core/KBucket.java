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
    private final LeastRecentlySeenSet bucket;
    private final MostRecentlySeenSet cache;

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

    public KBucketChangeSet touch(Instant time, Node node) throws EntryConflictException {
        Validate.notNull(time);
        Validate.notNull(node);

        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(nodeId.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        ChangeSet bucketTouchRes = bucket.touch(time, node);
        Validate.validState(bucketTouchRes.viewRemoved().isEmpty()); // sanity check, should never remove anything when touching bucket
        if (!bucketTouchRes.viewAdded().isEmpty() || !bucketTouchRes.viewUpdated().isEmpty()) {
            // node was added to bucket, or node was already in bucket and was updated
            lastUpdateTime = time;
            return new KBucketChangeSet(bucketTouchRes, ChangeSet.NO_CHANGE);
        }

        ChangeSet cacheTouchRes = cache.touch(time, node);
        lastUpdateTime = time;
        return new KBucketChangeSet(bucketTouchRes, cacheTouchRes);
    }
    
    public KBucketChangeSet replace(Instant time, Node node) throws EntryConflictException {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();

        Validate.isTrue(!nodeId.equals(baseId));
        Validate.isTrue(nodeId.getBitLength() == baseId.getBitLength());
        
        Validate.isTrue(nodeId.getBitString().getBits(0, prefix.getBitLength()).equals(prefix)); // ensure prefix matches

        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime

        if (cache.size() == 0 || bucket.size() == bucket.getMaxSize()) {
            return new KBucketChangeSet(ChangeSet.NO_CHANGE, ChangeSet.NO_CHANGE);
        }
        
        // Remove
        ChangeSet bucketRemoveRes = bucket.remove(node); // throws EntryConflictException if id is equal but link isn't
        if (bucketRemoveRes.viewRemoved().isEmpty()) {
            return new KBucketChangeSet(ChangeSet.NO_CHANGE, ChangeSet.NO_CHANGE);
        }
        
        // Remove latest from cache and add to bucket
        ChangeSet cacheLatestRes = cache.removeMostRecent(1);
        ChangeSet bucketTouchRes;
        Validate.validState(cacheLatestRes.viewRemoved().size() == 1); // sanity check, should always remove 1 node
        Entry cacheEntry = cacheLatestRes.viewRemoved().get(0);
        try {
            bucketTouchRes = bucket.touch(cacheEntry.getLastSeenTime(), cacheEntry.getNode());
        } catch (EntryConflictException ece) {
            // should never throw EntryConflictException
            throw new IllegalStateException(ece);
        }
        Validate.validState(bucketTouchRes.viewAdded().size() == 1); // sanity check, should always add 1 node
        
        
        return new KBucketChangeSet(bucketTouchRes, cacheLatestRes);
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

    // bitCount = 0 is 1 buckets
    // bitCount = 1 is 2 buckets
    // bitCount = 2 is 4 buckets
    // bitCount = 3 is 8 buckets
    public KBucket[] split(int bitCount) {
        Validate.isTrue(bitCount >= 0); // why would anyone want to split in to 1 bucket? the result would just be a copy of this bucket...
                                        // let through anyway
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
        
        
        // Move from original bucket to new buckets
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
            
            // Touch bucket
            ChangeSet res;
            try {
                res = newKBuckets[idx].bucket.touch(entry.getLastSeenTime(), node);
            } catch (EntryConflictException ece) {
                // should never happen
                throw new IllegalStateException(ece);
            }
            Validate.validState(!res.viewAdded().isEmpty()); // sanity check, should always add
            
            // Update lastUpdateTime if entry's timestamp is greater than the kbucket's timestamp
            if (entry.getLastSeenTime().isAfter(newKBuckets[idx].lastUpdateTime)) {
                newKBuckets[idx].lastUpdateTime = entry.getLastSeenTime();
            }
        }

        
        // Move from original cache to new cache
        for (Entry entry : cache.dump()) {
            Node node = entry.getNode();
            
            // Read bitCount bits starting from prefixBitSize and use that to figure out which bucket to copy to
            // For example, if bitCount is 2 ...
            // If you read 00b, 00 = 0, so this ID will be go to newBucket[0]
            // If you read 01b, 01 = 1, so this ID will be go to newBucket[1]
            // If you read 10b, 10 = 2, so this ID will be go to newBucket[2]
            // If you read 11b, 11 = 3, so this ID will be go to newBucket[3]
            Id id = node.getId();
            int idx = (int) id.getBitsAsLong(prefix.getBitLength(), bitCount);
            
            // Touch cache
            ChangeSet res;
            try {
                res = newKBuckets[idx].cache.touch(entry.getLastSeenTime(), node);
            } catch (EntryConflictException ece) {
                // should never happen
                throw new IllegalStateException(ece);
            }
            Validate.validState(!res.viewAdded().isEmpty()); // sanity check, should always add
            
            // Update lastUpdateTime if entry's timestamp is greater than the kbucket's timestamp
            if (entry.getLastSeenTime().isAfter(newKBuckets[idx].lastUpdateTime)) {
                newKBuckets[idx].lastUpdateTime = entry.getLastSeenTime();
            }
        }
        
        
        // Now that this is has been split in to multiple kbuckets, each kbucket's bucket may not be as full as possible. Try to move over
        // nodes from the cache to the bucket
        for (int i = 0; i < len; i++) {
            newKBuckets[i].fillMissingBucketSlotsWithCacheItems();
        }
        
        
        return newKBuckets;
    }
    
    public KBucketChangeSet resizeBucket(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        if (maxSize <= bucket.getMaxSize()) {
            // reducing space
            ChangeSet res = bucket.resize(maxSize);
            
            // sanity check
            // validate nothing was added or updated -- the only thing that can happen is elements can be removed
            Validate.validState(res.viewAdded().isEmpty());
            Validate.validState(res.viewUpdated().isEmpty());
            
            return new KBucketChangeSet(res, ChangeSet.NO_CHANGE);
        } else {
            // increasing space, so move over stuff from the cache in to new bucket spaces
            ChangeSet res = bucket.resize(maxSize);
            
            // sanity check
            // validate nothing changed with elements in the set -- we're only expanding the size of the bucket
            Validate.validState(res.viewAdded().isEmpty());
            Validate.validState(res.viewRemoved().isEmpty());
            Validate.validState(res.viewUpdated().isEmpty());
            
            
            return fillMissingBucketSlotsWithCacheItems();
        }
    }
    
    public KBucketChangeSet resizeCache(int maxSize) {
        Validate.isTrue(maxSize >= 0);
        
        ChangeSet res = cache.resize(maxSize);
        return new KBucketChangeSet(ChangeSet.NO_CHANGE, res);
    }

    public List<Entry> dumpBucket() {
        return bucket.dump();
    }

    public List<Entry> dumpCache() {
        return cache.dump();
    }
    
    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    private KBucketChangeSet fillMissingBucketSlotsWithCacheItems() {
        int unoccupiedBucketSlots = bucket.getMaxSize() - bucket.size();
        int availableCacheItems = cache.size();
        if (unoccupiedBucketSlots <= 0 || availableCacheItems == 0) {
            return new KBucketChangeSet(ChangeSet.NO_CHANGE, ChangeSet.NO_CHANGE);
        }
        
        int moveAmount = Math.min(availableCacheItems, unoccupiedBucketSlots);
        
        ChangeSet cacheRemoveRes = cache.removeMostRecent(moveAmount);
        Validate.validState(cacheRemoveRes.viewAdded().isEmpty());
        Validate.validState(cacheRemoveRes.viewRemoved().size() == moveAmount); // sanity check
        Validate.validState(cacheRemoveRes.viewUpdated().isEmpty());

        for (Entry entryToMove : cacheRemoveRes.viewRemoved()) {
            // move
            ChangeSet addRes;
            try {
                addRes = bucket.touch(entryToMove.getLastSeenTime(), entryToMove.getNode());
            } catch (EntryConflictException ece) {
                // This should never happen. The way the logic in this class is written, you should never have an entry with the same id in
                // the cache and the bucket at the same time. As such, it's impossible to encounter a conflict.
                throw new IllegalStateException(ece); // sanity check
            }
            
            // sanity check
            Validate.validState(addRes.viewAdded().size() == 1);
            Validate.validState(addRes.viewRemoved().isEmpty());
            Validate.validState(addRes.viewUpdated().isEmpty());
        }
        
        // show moved as being added to bucket and removed from cache
        return new KBucketChangeSet(ChangeSet.added(cacheRemoveRes.viewRemoved()), cacheRemoveRes);
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
        Validate.isTrue(bitLength >= 0);

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
}
