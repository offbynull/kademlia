package com.offbynull.voip.core;

import java.time.Instant;
import org.apache.commons.lang3.Validate;


public final class KBucketEntry {
    private final NodeInformation nodeInfo;
    private final Instant lastSeenTime;
    private final Instant insertTime;

    public KBucketEntry(NodeInformation nodeInfo, Instant insertTime, Instant lastSeenTime) {
        Validate.notNull(nodeInfo);
        Validate.notNull(lastSeenTime);
        Validate.notNull(insertTime);
        this.nodeInfo = nodeInfo;
        this.lastSeenTime = lastSeenTime;
        this.insertTime = insertTime;
    }

    public NodeInformation getNodeInfo() {
        return nodeInfo;
    }

    public Instant getLastSeenTime() {
        return lastSeenTime;
    }

    public Instant getInsertTime() {
        return insertTime;
    }

    public KBucketEntry updateLastSeenTime(Instant lastSeenTime) {
        Validate.notNull(lastSeenTime);
        Validate.isTrue(!this.lastSeenTime.isAfter(lastSeenTime));
        return new KBucketEntry(nodeInfo, insertTime, lastSeenTime);
    }
    
}
