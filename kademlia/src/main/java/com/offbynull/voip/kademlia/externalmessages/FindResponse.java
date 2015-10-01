/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Node;
import java.io.Serializable;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

/**
 * A response to a {@link FindRequest} message. Must contain the closest nodes to the ID that was being searched for by searching the
 * replying Kademlia node's routing tree.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class FindResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Node[] nodes;

    /**
     * Constructs a {@link FindResponse} object.
     * @param nodes closest nodes to the ID that was searched for
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public FindResponse(Node[] nodes) {
        Validate.notNull(nodes);
        Validate.noNullElements(nodes);
        this.nodes = Arrays.copyOf(nodes, nodes.length);
    }

    /**
     * Get closest nodes to the ID that was searched for.
     * @return closest nodes
     */
    public Node[] getNodes() {
        return Arrays.copyOf(nodes, nodes.length);
    }
    
}
