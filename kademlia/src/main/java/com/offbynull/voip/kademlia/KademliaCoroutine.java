package com.offbynull.voip.kademlia;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.shuttle.Address;

public final class KademliaCoroutine implements Coroutine {

    @Override
    public void run(Continuation cnt) throws Exception {
        new KademliaSubcoroutine(Address.of()).run(cnt);
    }
}
