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
package com.offbynull.voip.core;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Kademlia k-bucket. A k-bucket can hold up to a certain number of nodes (defined as k in the original Kademlia paper), where each node has
 * a shared common prefix of n bits with the base ID.
 * @author Kasra Faghihi
 */
public final class Bucket {
    private final Id baseId;
    private final int commonPrefixSize; // For something to be allowed in this bucket, it needs to share a common prefix of this many bits
                                        // with baseId field

    private final int maxBucketSize; // k -- k is a system-wide replication parameter. k is chosen such that any given k nodes are very
                                     // unlikely to fail within an hour of eachother (for example k = 20)
    
    private final LinkedList<Entry> entries; // Each k-bucket is kept sorted by time last seen
    
    private Instant lastUpdateTime;

    public Bucket(Id baseId, int commonPrefixSize, int maxBucketSize) {
        Validate.notNull(baseId);
        Validate.isTrue(commonPrefixSize >= 0);
        Validate.isTrue(commonPrefixSize <= baseId.getBitLength());
        Validate.isTrue(maxBucketSize > 0);
        
        this.baseId = baseId;
        this.commonPrefixSize = commonPrefixSize;
        this.maxBucketSize = maxBucketSize;

        this.entries = new LinkedList<>();
        
        lastUpdateTime = Instant.MIN;
    }
  
    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Validate.isTrue(baseId.getBitLength() == nodeId.getBitLength());
        Validate.isTrue(baseId.getSharedPrefixLength(nodeId) >= commonPrefixSize);
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        // If not true, this essentially means that this.id and id are equal, so what's the point of having a bucket? Validate here?
        // Validate.isTrue(commonPrefixBitCount < id.getBitLength());
        
        // Update if already exists
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            
            Id entryId = entry.getNode().getId();
            String entryLink = entry.getNode().getLink();
            
            if (nodeId.equals(entryId)) {
                if (!nodeLink.equals(entryLink)) {
                    // if ID exists but link for ID is different, ignore
                    return TouchResult.IGNORED;
                }
                
                // remove and add to tail (most recently-seen)
                entry.setLastSeenTime(time);
                it.remove();
                entries.addLast(entry);
                lastUpdateTime = time;
                return TouchResult.UPDATED;
            }
        }
        
        // Stop if full
        if (entries.size() == maxBucketSize) {
            return TouchResult.FULL;
        }
        
        // Add
        Entry newEntry = new Entry(node, time);
        entries.addLast(newEntry);
        lastUpdateTime = time;
        return TouchResult.INSERTED;
    }

    public ReplaceResult replace(Instant time, Id replaceId, Node newNode) {
        Validate.notNull(time);
        Validate.notNull(replaceId);
        Validate.notNull(newNode);
        
        Validate.isTrue(replaceId.getBitLength() == baseId.getBitLength());
        Validate.isTrue(replaceId.getSharedPrefixLength(baseId) >= commonPrefixSize);
        
        Validate.isTrue(newNode.getId().getBitLength() == baseId.getBitLength());
        Validate.isTrue(newNode.getId().getSharedPrefixLength(baseId) >= commonPrefixSize);
        
        Validate.isTrue(!time.isBefore(lastUpdateTime)); // time must be >= lastUpdatedTime
        
        ListIterator<Entry> it = entries.listIterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.getNode().getId().equals(replaceId)) {
                lastUpdateTime = time;
                it.set(new Entry(newNode, time));
                return ReplaceResult.REPLACED;
            }
        }
        
        return ReplaceResult.NOT_FOUND;
    }

    public List<Node> getClosest(Id id, int count) {
        Validate.notNull(id);
        Validate.isTrue(count >= 0);

        Validate.isTrue(id.getBitLength() == baseId.getBitLength());
        Validate.isTrue(id.getSharedPrefixLength(baseId) >= commonPrefixSize);
        
        return entries.stream()
                .map(x -> new ImmutablePair<>(x, x.getNode().getId().getSharedPrefixLength(id)))
                .sorted((x, y) -> Integer.compare(x.getValue(), y.getValue()))
                .map(x -> x.getKey().getNode())
                .limit(count)
                .collect(Collectors.toList());
        
        // QUESTION: Before returning, should we check further? According to the paper... "the closest leaf to an ID x is the leaf whos ID
        // shares the longest common prefix of x. If there are empty branches in the tree, there might be more than one leaf with the
        // longest common prefix. In that case, the closest leaf to x will be the closest leaf to ID ~x produced by flipping the bits in x
        // corresponding to the empty branches of the tree."
        //
        // Probably not, as bit flipping seems specifically for finding the bucket in the routing table. At this stage we likely just want
        // the n nodes with the longest matching prefix. If some of those n nodes share a longest matching prefix, does it matter? doesn't
        // seem like it does.
    }

    public Node get(int i) {
        Validate.isTrue(i >= 0);
        Validate.isTrue(i < entries.size());

        return entries.get(i).getNode();
    }

    public int size() {
        return entries.size();
    }

    // bitCount = 1 is 2 buckets
    // bitCount = 2 is 4 buckets
    // bitCount = 3 is 8 buckets
    public Bucket[] split(int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to split in to 2^30 buckets, but whatever
                                         // we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new KBucket[1 << 31] -- 1 << 31 is negative
                                         // new KBucket[1 << 30] -- 1 << 30 is positive
        
        Validate.isTrue(commonPrefixSize + bitCount <= baseId.getBitLength());

        // Create buckets
        int len = 1 << bitCount;
        Bucket[] newBuckets = new Bucket[len];
        for (int prefixAppendage = 0; prefixAppendage < newBuckets.length; prefixAppendage++) {
            // Append prefixAppendage to baseId to create the base id for split bucket
            // TODO: Make a more efficient method to do this, unnecessary creation of IDs on setBit
            Id newBucketBaseId = baseId;
            for (int i = 0; i < bitCount; i++) {
                int shiftLen = (bitCount - i) - 1;
                boolean nextBit = ((prefixAppendage >>> shiftLen) & 0x1) == 0x1;
                newBucketBaseId = newBucketBaseId.setBit(commonPrefixSize + i, nextBit);
            }
            
            newBuckets[prefixAppendage] = new Bucket(newBucketBaseId, commonPrefixSize + bitCount, maxBucketSize);
        }
        
        // Place entries in buckets
        for (Entry entry : entries) {
            Node node = entry.getNode();
            
            // Read bitCount bits starting from prefixBitSize and use that to figure out which bucket to copy to
            // For example, if bitCount is 2 ...
            // If you read 00b, 00 = 0, so this ID will be go to newBucket[0]
            // If you read 01b, 01 = 1, so this ID will be go to newBucket[1]
            // If you read 10b, 10 = 2, so this ID will be go to newBucket[2]
            // If you read 11b, 11 = 3, so this ID will be go to newBucket[3]
            Id id = node.getId();
            int idx = 0;
            for (int i = 0; i < bitCount; i++) {
                int shiftLen = (bitCount - i) - 1;
                int idBitPos = commonPrefixSize + i;
                idx |= (id.getBit(idBitPos) ? 1 : 0) << shiftLen;
            }
            
            TouchResult res;
            res = newBuckets[idx].touch(entry.getInsertTime(), node); // first call to touch should add with insert time
            Validate.validState(res == TouchResult.INSERTED); // should always happen, but just in case
            res = newBuckets[idx].touch(entry.getLastSeenTime(), node); // send call to touch should set laset seen time
            Validate.validState(res == TouchResult.UPDATED); // should always happen, but just in case
        }
        
        return newBuckets;
    }

    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    public enum ReplaceResult {
        REPLACED, // replaced
        NOT_FOUND // id to replace couldn't be found
    }
    
    public enum TouchResult {
        INSERTED, // inserted as latest entry
        UPDATED, // entry already existed and has been updated
        IGNORED, // entry with same id already existed, but link is different, so ignoring
        FULL // latest entry needs to be pinged to see if its still alive, if it isn't remove then update again to add this guy back in
    }

    @Override
    public String toString() {
        StringBuilder prefixSb = new StringBuilder();
        for (int i = 0; i < commonPrefixSize; i++) {
            prefixSb.append(baseId.getBit(i) ? 1 : 0);
        }
        
        int totalSize = baseId.getBitLength();
        for (int i = commonPrefixSize; i < totalSize; i++) {
            prefixSb.append('x');
        }
        
        return "Bucket{" + "baseId=" + prefixSb + ", commonPrefixSize=" + commonPrefixSize + ", maxBucketSize=" + maxBucketSize
                + ", entries=" + entries + ", lastUpdateTime=" + lastUpdateTime + '}';
    }

}
