package com.offbynull.voip.kademlia.externalmessages;

import com.offbynull.voip.kademlia.model.Id;
import java.io.Serializable;
import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class FindResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Id[] ids;

    public FindResponse(Id[] ids) {
        Validate.notNull(ids);
        Validate.noNullElements(ids);
        this.ids = ids;
    }

    public Id[] getIds() {
        return Arrays.copyOf(ids, ids.length);
    }
    
}
