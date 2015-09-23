package com.offbynull.voip.kademlia.internalmessages;

import com.offbynull.voip.kademlia.model.Node;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class SearchResponse {
    private final Node[] nodes;

    public SearchResponse(Node[] nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        this.nodes = nodes;
    }

    public Node[] getNodes() {
        return Arrays.copyOf(nodes, nodes.length);
    }

    @Override
    public String toString() {
        return "SearchResponse{" + "nodes=" + Arrays.toString(nodes) + '}';
    }
    
}
