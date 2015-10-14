package com.offbynull.voip.audio.actors.stream.externalmessages;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class AudioBlock {
    private final int sequence;
    private final byte[] data;

    public AudioBlock(int sequence, byte[] data) {
        Validate.notNull(data);
        this.sequence = sequence;
        this.data = Arrays.copyOf(data, data.length);
    }

    public int getSequence() {
        return sequence;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
    
}
