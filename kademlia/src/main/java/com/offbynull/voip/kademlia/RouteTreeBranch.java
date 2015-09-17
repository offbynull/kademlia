package com.offbynull.voip.kademlia;

public interface RouteTreeBranch {
    BitString getPrefix();
    <T> T getItem(); // why type parameter? hack to prevent explicit casting
}
