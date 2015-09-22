package com.offbynull.voip.kademlia;

import com.offbynull.peernetic.core.shuttle.Address;

final class AddressConstants {
    // in case you forget, this is all threadsafe... for more information read
    // http://stackoverflow.com/questions/8865086/why-is-this-static-final-variable-in-a-singleton-thread-safe
    
    private AddressConstants() {
    }
    
    public static final String JOIN_ELEMENT_NAME = "join";
    public static final String ROUTER_ELEMENT_NAME = "router";
    public static final String ADVERTISE_ELEMENT_NAME = "advertise";
    public static final String REFRESH_ELEMENT_NAME = "refresh";
    public static final String EXT_HANDLER_ELEMENT_NAME = "externalhandler";
    public static final String INT_HANDLER_ELEMENT_NAME = "internalhandler";

    public static final Address JOIN_RELATIVE_ADDRESS = Address.of(JOIN_ELEMENT_NAME);
    public static final Address ROUTER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME);
    public static final Address ROUTER_ADVERTISE_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, ADVERTISE_ELEMENT_NAME);
    public static final Address ROUTER_REFRESH_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, REFRESH_ELEMENT_NAME);
    public static final Address ROUTER_EXT_HANDLER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, EXT_HANDLER_ELEMENT_NAME);
    public static final Address ROUTER_INT_HANDLER_RELATIVE_ADDRESS = Address.of(ROUTER_ELEMENT_NAME, INT_HANDLER_ELEMENT_NAME);
    
}
