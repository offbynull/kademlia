package com.offbynull.voip.audio.internalmessages;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public class InputPCMBlock {
    private final byte[] data;

    public InputPCMBlock(byte[] data) {
        Validate.notNull(data);
        this.data = Arrays.copyOf(data, data.length);
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
}
