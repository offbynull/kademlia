package com.offbynull.voip.core;

import java.util.Comparator;

public final class KBucketEntryLastSeenTimeComparator implements Comparator<KBucketEntry> {

    @Override
    public int compare(KBucketEntry o1, KBucketEntry o2) {
        return o1.getLastSeenTime().compareTo(o2.getLastSeenTime());
    }
    
}
