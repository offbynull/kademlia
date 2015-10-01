package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

public final class PingResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id id;

    public PingResponse(Id id) {
        Validate.notNull(id);
        this.id = id;
    }

    public Id getId() {
        return id;
    }
    
}
