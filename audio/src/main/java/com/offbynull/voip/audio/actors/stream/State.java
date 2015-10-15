/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.voip.audio.actors.stream;

import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.actor.helpers.AddressTransformer;
import org.apache.commons.lang3.Validate;


final class State {

    private final AddressTransformer addressTransformer;
    private final Address audioIoAddress;
    private final Address timerAddress;
    private final Address logAddress;
    
    public State(
            AddressTransformer addressTransformer,
            Address audioIoAddress,
            Address timerAddress,
            Address logAddress) {
        Validate.notNull(addressTransformer);
        Validate.notNull(audioIoAddress);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        this.addressTransformer = addressTransformer;
        this.audioIoAddress = audioIoAddress;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
    }

    public AddressTransformer getAddressTransformer() {
        return addressTransformer;
    }

    public Address getAudioIoAddress() {
        return audioIoAddress;
    }

    public Address getTimerAddress() {
        return timerAddress;
    }

    public Address getLogAddress() {
        return logAddress;
    }

}
