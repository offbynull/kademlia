package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;

public abstract class KademliaRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id fromId;

    public KademliaRequest(Id fromId) {
        // fromId can be null -- if null it means that the node is not technically part of the network (its either in the process of joining
        // or its just browsing the network rather than being a part of it).
        this.fromId = fromId;
    }

    public Id getFromId() {
        return fromId;
    }
}
