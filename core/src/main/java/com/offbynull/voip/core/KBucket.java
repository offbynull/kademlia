package com.offbynull.voip.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.Comparator;
import java.util.TreeSet;
import org.apache.commons.lang3.Validate;

public final class KBucket {
    private final Id id;
    
    // k -- k is a system-wide replication parameter. k is chosen such that any given k nodes are very unlikely to fail within an hour of eachother (for example k = 20)
    private final int maxSize;
    
    // i -- For each 0 <= i < 160, everynode keeps a list of (ip,port,id) triples for nodes of distance between 2^i and 2^(i+1) from itself
    // 160 is the exponent size for the keyspace (e.g. max key is 2^160 - 1)
    private final int exponentPos; // i
    private final Id startId; // 2^i
    private final Id endId; // 2^(i+1)
      
      // Each k-bucket is kept sorted by time last seen
    private final TreeSet<Entry> nodes;

    public KBucket(Id id, int maxSize, int exponentPos) {
        Validate.notNull(id);
        Validate.isTrue(maxSize > 0);
        
        int exponentPosMax = id.getLimitAsExponent();
        
        Validate.isTrue(exponentPos >= 0);
        Validate.isTrue(exponentPos < exponentPosMax);
        
        this.id = id;
        this.maxSize = maxSize;
        this.exponentPos = exponentPos;
        
        this.startId = Id.xor(id, Id.createExponent(exponentPos, exponentPosMax));
        this.endId = Id.xor(id, Id.createExponent(exponentPos + 1, exponentPosMax));
        
        this.nodes = new TreeSet<>(new EntryComparator());
    }
    
    
    private static final class Entry {
        private final NodeInformation nodeInfo;
        private final Instant lastSeen;

        public Entry(NodeInformation nodeInfo, Instant lastSeen) {
            Validate.notNull(nodeInfo);
            Validate.notNull(lastSeen);
            this.nodeInfo = nodeInfo;
            this.lastSeen = lastSeen;
        }

        public NodeInformation getNodeInfo() {
            return nodeInfo;
        }

        public Instant getLastSeen() {
            return lastSeen;
        }
        
    }
    
    private static final class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry o1, Entry o2) {
            return o1.getLastSeen().compareTo(o2.getLastSeen());
        }
    
    }
}
