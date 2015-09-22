package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;

public final class PingRequest extends KademliaRequest implements Serializable {
    private static final long serialVersionUID = 1L;  

    public PingRequest(Id fromId) {
        super(fromId);
    }
}
