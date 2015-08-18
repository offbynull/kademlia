package com.offbynull.voip.core;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class NodeInformation {
    private final Address address; // equivalent to ip address + udp port
    private final Id id;

    public NodeInformation(Address address, Id id) {
        Validate.notNull(address);
        Validate.notNull(id);
        Validate.isTrue(!address.isEmpty());
        this.address = address;
        this.id = id;
    }

    public Address getAddress() {
        return address;
    }

    public Id getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.address);
        hash = 83 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeInformation other = (NodeInformation) obj;
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
