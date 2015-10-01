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
