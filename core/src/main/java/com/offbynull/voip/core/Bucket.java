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
    private final BitString prefix;
    private final int idBitLength;

    private final int maxSize; // k -- k is a system-wide replication parameter. k is chosen such that any given k nodes are very unlikely
                               // to fail within an hour of eachother (for example k = 20
    
    private final LinkedList<Entry> entries; // Each k-bucket is kept sorted by time last seen
    
    private Instant lastUpdateTime;

    public Bucket(BitString prefix, int idBitLength, int maxSize) {
        Validate.notNull(prefix);
        Validate.isTrue(idBitLength >= prefix.getBitLength());
        Validate.isTrue(maxSize >= 0);
        
        this.prefix = prefix;
        this.idBitLength = idBitLength;
        this.maxSize = maxSize;

        this.entries = new LinkedList<>();
        
        lastUpdateTime = Instant.MIN;
    }
  
    public TouchResult touch(Instant time, Node node) {
        Validate.notNull(time);
        Validate.notNull(node);
        
        Id nodeId = node.getId();
        String nodeLink = node.getLink();
        
        Validate.isTrue(nodeId.getBitLength() == idBitLength);
        Validate.isTrue(nodeId.getBitString().getSharedPrefixLength(prefix) == prefix.getBitLength());
        
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
        if (entries.size() == maxSize) {
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
        
        Validate.isTrue(replaceId.getBitLength() == idBitLength);
        Validate.isTrue(replaceId.getBitString().getSharedPrefixLength(prefix) == prefix.getBitLength());

        Validate.isTrue(newNode.getId().getBitLength() == idBitLength);
        Validate.isTrue(newNode.getId().getBitString().getSharedPrefixLength(prefix) == prefix.getBitLength());

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

        Validate.isTrue(id.getBitLength() == idBitLength);
        Validate.isTrue(id.getBitString().getSharedPrefixLength(prefix) == prefix.getBitLength());
        
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

    public int getMaxSize() {
        return maxSize;
    }

    // bitCount = 1 is 2 buckets
    // bitCount = 2 is 4 buckets
    // bitCount = 3 is 8 buckets
    public Bucket[] split(int bitCount) {
        Validate.isTrue(bitCount >= 1);
        Validate.isTrue(bitCount <= 30); // its absurd to check for this, as no one will ever want to split in to 2^30 buckets, but whatever
                                         // we can't have more than 30 bits, because 31 bits will result in an array of neg size...
                                         // new Bucket[1 << 31] -- 1 << 31 is negative
                                         // new Bucket[1 << 30] -- 1 << 30 is positive
        
        Validate.isTrue(prefix.getBitLength() + bitCount <= idBitLength);

        // Create buckets
        BitString[] newPrefixes = InternalUtils.appendToBitString(prefix, bitCount);
        Bucket[] newBuckets = new Bucket[newPrefixes.length];
        for (int i = 0; i < newBuckets.length; i++) {
            newBuckets[i] = new Bucket(newPrefixes[i], idBitLength, maxSize);
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
            int idx = (int) id.getBitsAsLong(prefix.getBitLength(), bitCount);
            
            TouchResult res;
            res = newBuckets[idx].touch(entry.getInsertTime(), node); // first call to touch should add with insert time
            Validate.validState(res == TouchResult.INSERTED); // should always happen, but just in case
            res = newBuckets[idx].touch(entry.getLastSeenTime(), node); // send call to touch should set laset seen time
            Validate.validState(res == TouchResult.UPDATED); // should always happen, but just in case
        }
        
        return newBuckets;
    }

    public Bucket resize(int maxSize) {
        Bucket ret = new Bucket(prefix, idBitLength, maxSize);
        
        int end = Math.min(maxSize, entries.size());
        int start = this.maxSize - end;
        
        for (Entry entry : entries.subList(start, end)) {
            Node node = entry.getNode();
            
            TouchResult res;
            res = ret.touch(entry.getInsertTime(), node); // first call to touch should add with insert time
            Validate.validState(res == TouchResult.INSERTED); // should always happen, but just in case
            res = ret.touch(entry.getLastSeenTime(), node); // send call to touch should set laset seen time
            Validate.validState(res == TouchResult.UPDATED); // should always happen, but just in case
        }
        
        return ret;
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
        return "Bucket{" + "prefix=" + prefix + ", idBitLength=" + idBitLength + ", maxBucketSize=" + maxSize + ", entries=" + entries
                + ", lastUpdateTime=" + lastUpdateTime + '}';
    }

}
