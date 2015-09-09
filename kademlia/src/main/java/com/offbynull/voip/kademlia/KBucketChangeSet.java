package com.offbynull.voip.kademlia;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class KBucketChangeSet {
    private final EntryChangeSet bucketChangeSet;
    private final EntryChangeSet cacheChangeSet;

    public KBucketChangeSet(EntryChangeSet bucketChangeSet, EntryChangeSet cacheChangeSet) {
        Validate.notNull(bucketChangeSet);
        Validate.notNull(cacheChangeSet);
        this.bucketChangeSet = bucketChangeSet;
        this.cacheChangeSet = cacheChangeSet;
    }

    public EntryChangeSet getBucketChangeSet() {
        return bucketChangeSet;
    }

    public EntryChangeSet getCacheChangeSet() {
        return cacheChangeSet;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.bucketChangeSet);
        hash = 83 * hash + Objects.hashCode(this.cacheChangeSet);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KBucketChangeSet other = (KBucketChangeSet) obj;
        if (!Objects.equals(this.bucketChangeSet, other.bucketChangeSet)) {
            return false;
        }
        if (!Objects.equals(this.cacheChangeSet, other.cacheChangeSet)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "KBucketChangeSet{" + "bucketChangeSet=" + bucketChangeSet + ", cacheChangeSet=" + cacheChangeSet + '}';
    }
    
}
