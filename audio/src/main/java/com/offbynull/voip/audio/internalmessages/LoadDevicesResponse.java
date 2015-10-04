package com.offbynull.voip.audio.internalmessages;

import org.apache.commons.lang3.Validate;

public final class LoadDevicesResponse {
    
    public static final class OutputDevice {
        private final int id;
        private final String name;

        public OutputDevice(int id, String name) {
            Validate.notNull(name);
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static final class InputDevice {
        private final int id;
        private final String name;

        public InputDevice(int id, String name) {
            Validate.notNull(name);
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
