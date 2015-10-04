package com.offbynull.voip.audio;

import java.util.Arrays;
import org.apache.commons.lang3.Validate;

public final class OutputData {
    private final byte[] data;

    public OutputData(byte[] data) {
        Validate.notNull(data);
        this.data = Arrays.copyOf(data, data.length);
    }

    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }
    
}