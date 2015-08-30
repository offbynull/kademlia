package com.offbynull.voip.core;

import java.time.Instant;
import org.apache.commons.lang3.Validate;


public final class KBucketEntry {
    private final Id id;
    private final String link;
    private final Instant insertTime;
    private Instant lastSeenTime;
    private boolean stale;

    public KBucketEntry(Id id, String link, Instant insertTime) {
        Validate.notNull(id);
        Validate.notNull(link);
        Validate.notNull(insertTime);
        this.id = id;
        this.link = link;
        this.lastSeenTime = insertTime;
        this.insertTime = insertTime;
    }

    public Id getId() {
        return id;
    }

    public String getLink() {
        return link;
    }

    public Instant getLastSeenTime() {
        return lastSeenTime;
    }

    public Instant getInsertTime() {
        return insertTime;
    }

    public void setLastSeenTime(Instant lastSeenTime) {
        Validate.notNull(lastSeenTime);
        Validate.isTrue(!this.lastSeenTime.isAfter(lastSeenTime));
        this.lastSeenTime = lastSeenTime;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }
}
