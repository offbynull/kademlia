package com.offbynull.voip.core;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class KademliaRoutingTable {
    private final Id id;
    private final Map<BigInteger, Object> subtree;
    
    public KademliaRoutingTable(Id id, int prefixSize) {
        Validate.notNull(id);
        Validate.isTrue(prefixSize > 0);
        Validate.isTrue(prefixSize <= id.getBitLength());
        
        this.id = id;
        this.subtree = new HashMap<>();
        
        BigInteger prefix = id.getValuePrefixAsBigInteger(prefixSize);
        for (int i = 0; i <= prefixSize; i--) {
            BigInteger subtreePrefix = prefix.shiftRight(i);
            subtreePrefix = subtreePrefix.flipBit(0);
            
            subtree.put(subtreePrefix, "PUT BUCKET HERE"); // map is probably the wrong datastructure, should probably jsut be a ordered list as size of prefix is unique
        }
    }
}
