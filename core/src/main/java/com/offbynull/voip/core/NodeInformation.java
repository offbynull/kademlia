package com.offbynull.voip.core;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

public final class NodeInformation {
    private final String link; // equivalent to ip address + udp port
    private final Id id;

    public NodeInformation(String link, Id id) {
        Validate.notNull(link);
        Validate.notNull(id);
        this.link = link;
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public Id getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.link);
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
        if (!Objects.equals(this.link, other.link)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
