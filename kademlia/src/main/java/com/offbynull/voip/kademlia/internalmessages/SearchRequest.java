package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.voip.kademlia.model.Id;
import org.apache.commons.lang3.Validate;

public final class SearchRequest {
    private final Id findId;
    private final int maxResults;

    public SearchRequest(Id fromId, Id findId, int maxResults) {
        Validate.notNull(findId);
        Validate.isTrue(maxResults >= 0); // why would anyone want 0? let thru anyway
        this.findId = findId;
        this.maxResults = maxResults;
    }
    
    public Id getFindId() {
        return findId;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
