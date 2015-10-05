package com.offbynull.voip.audio;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class InputData {
    private final byte[] data;

    public InputData(byte[] data, int len) {
        Validate.notNull(data);
        Validate.isTrue(len >= 0);
        Validate.isTrue(len <= data.length);
        this.data = Arrays.copyOf(data, len);
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
    
}
