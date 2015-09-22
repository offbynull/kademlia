package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.voip.kademlia.model.Id;
import org.apache.commons.lang3.Validate;

public final class SearchRequest {
    private final Id findId;
    private final int max;

    public SearchRequest(Id fromId, Id findId, int max) {
        Validate.notNull(findId);
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyway
        this.findId = findId;
        this.max = max;
    }
    
    public Id getFindId() {
        return findId;
    }

    public int getMax() {
        return max;
    }
}
