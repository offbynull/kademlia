package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class FindRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Id findId;
    private final int max;

    public FindRequest(Id findId, int max) {
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
