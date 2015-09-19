package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class FindRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id id;
    private final int max;

    public FindRequest(Id id, int max) {
        Validate.notNull(id);
        Validate.isTrue(max >= 0); // why would anyone want 0? let thru anyway
        this.id = id;
        this.max = max;
    }

    public Id getId() {
        return id;
    }

    public int getMax() {
        return max;
    }
    
}
