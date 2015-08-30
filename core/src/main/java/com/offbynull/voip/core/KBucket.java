/*
 * Copyright (c) 2013-2015, Kasra Faghihi, All rights reserved.
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
package com.offbynull.voip.core;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.Validate;

/**
 * Kademlia k-bucket. Retains nodes with some prefix
 * @author Kasra Faghihi
 */
public final class KBucket {
    private final Id id;
    private final int commonPrefixSize; // For something to be allowed in this bucket, it needs to share a common prefix of this many bits
                                        // with id field

    private final int bucketSize; // k -- k is a system-wide replication parameter. k is chosen such that any given k nodes are very
                                  // unlikely to fail within an hour of eachother (for example k = 20)
    
    private final List<KBucketEntry> entries; // Each k-bucket is kept sorted by time last seen
    
    private Instant lastUpdateTime;

    public KBucket(Id id, int commonPrefixSize, int bucketSize) {
        Validate.notNull(id);
        Validate.isTrue(commonPrefixSize >= 0);
        Validate.isTrue(commonPrefixSize <= id.getBitLength());
        Validate.isTrue(bucketSize > 0);
        
        this.id = id;
        this.commonPrefixSize = commonPrefixSize;
        this.bucketSize = bucketSize;

        this.entries = new LinkedList<>();
        
        lastUpdateTime = Instant.MIN;
    }
  
    public TouchResult touch(Instant time, Id id, String link) {
        Validate.notNull(time);
        Validate.notNull(id);
        Validate.notNull(link);
        Validate.isTrue(id.getBitLength() == this.id.getBitLength());
        Validate.isTrue(time.isAfter(lastUpdateTime));

        int commonPrefixBitCount = this.id.getSharedPrefixLength(id);
        Validate.isTrue(commonPrefixBitCount == commonPrefixSize);
        
        // If not true, this essentially means that this.id and id are equal, so what's the point of having a bucket? Validate here?
//        Validate.isTrue(commonPrefixBitCount < id.getBitLength());
        
        lastUpdateTime = time;
        
        // Update if already exists
        for (KBucketEntry entry : entries) {
            if (id.equals(entry.getId())) {
                if (!link.equals(entry.getLink())) {
                    // if ID exists but link for ID is different, ignore
                    return TouchResult.IGNORED;
                }
                
                entry.setLastSeenTime(time);
                entry.setStale(false); //in case was set to stale, it is no longer stale, because we just heard from it
                return TouchResult.UPDATED;
            }
        }
        
        // Stop if full
        if (entries.size() == bucketSize) {
            return TouchResult.FULL;
        }
        
        // Add
        KBucketEntry newEntry = new KBucketEntry(id, link, time);
        entries.add(newEntry);
        return TouchResult.INSERTED;
    }

    // bitCount = 1 is 2 buckets
    // bitCount = 2 is 4 buckets
    // bitCount = 3 is 8 buckets
    public KBucket[] split(int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to split in to 2^30 buckets, but whatever
                                         // we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new KBucket[1 << 31] -- 1 << 31 is negative
                                         // new KBucket[1 << 30] -- 1 << 30 is positive
        
        Validate.isTrue(commonPrefixSize + bitCount <= id.getBitLength());
        
        int len = 1 << bitCount;
        KBucket[] newBuckets = new KBucket[len];
        for (int i = 0; i < newBuckets.length; i++) {
            newBuckets[i] = new KBucket(id, commonPrefixSize + bitCount, bucketSize);
        }
        
        for (KBucketEntry entry : entries) {
            Id entryId = entry.getId();
            
            // Read bitCount bits starting from prefixBitSize and use that to figure out which bucket to copy to
            // For example, if bitCount is 2 ...
            // If you read 00b, 00 = 0, so this ID will be go to newBucket[0]
            // If you read 01b, 01 = 1, so this ID will be go to newBucket[1]
            // If you read 10b, 00 = 2, so this ID will be go to newBucket[2]
            // If you read 11b, 00 = 3, so this ID will be go to newBucket[3]
            int idx = 0;
            for (int i = 0; i < bitCount; i++) {
                idx |= (entryId.getBit(commonPrefixSize + i) ? 1 : 0) << i;
            }
            
            TouchResult res = newBuckets[idx].touch(entry.getInsertTime(), entry.getId(), entry.getLink());
            Validate.validState(res == TouchResult.INSERTED); // not required, but just in case
        }
        
        return newBuckets;
    }

    public enum TouchResult {
        INSERTED, // inserted as latest entry
        UPDATED, // entry already existed and has been updated
        IGNORED, // entry with same id already existed, but link is different, so ignoring
        FULL // latest entry needs to be pinged to see if its still alive, if it isn't remove then update again to add this guy back in
    }

}
