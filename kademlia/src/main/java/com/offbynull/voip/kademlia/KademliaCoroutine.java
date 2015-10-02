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
package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.voip.kademlia.internalmessages.Start;

/**
 * A Kademlia coroutine actor.
 * <p>
 * To use this actor, issue it messages from the {@link com.offbynull.voip.kademlia.internalmessages} package. This actor must be primed
 * using a {@link Start} message.
 * <p>
 * If you want to run Kademlia as part of another actor, use {@link KademliaSubcoroutine} instead.
 * @author Kasra Faghihi
 */
public final class KademliaCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        new KademliaSubcoroutine(Address.of()).run(cnt);
    }
}
