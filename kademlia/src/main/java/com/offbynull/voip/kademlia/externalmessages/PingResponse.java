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

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * A response to a {@link PingRequest} message.
 * <p>
 * This class is immutable.
 * @author Kasra Faghihi
 */
public final class PingResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id id;

    /**
     * Constructs a {@link PingResponse} message.
     * @param id id of the Kademlia actor that's responding
     * @throws NullPointerException if any argument is {@code null}
     */
    public PingResponse(Id id) {
        Validate.notNull(id);
        this.id = id;
    }

    /**
     * Get the ID of the Kademlia node that generated/sent this response.
     * @return ID of the responder
     */
    public Id getId() {
        return id;
    }
    
}
