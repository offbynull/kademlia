package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class FindRequest extends KademliaRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Id findId;
    private final int maxResults;

    public FindRequest(Id fromId, Id findId, int maxResults) {
        super(fromId);
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
