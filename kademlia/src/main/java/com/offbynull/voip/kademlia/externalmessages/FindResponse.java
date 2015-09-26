package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Node;
import java.io.Serializable;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class FindResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Node[] nodes;

    public FindResponse(Node[] nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        this.nodes = Arrays.copyOf(nodes, nodes.length);
    }

    public Node[] getNodes() {
        return Arrays.copyOf(nodes, nodes.length);
    }
    
}
