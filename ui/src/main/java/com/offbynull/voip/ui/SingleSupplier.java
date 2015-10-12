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
package com.offbynull.voip.ui;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

final class SingleSupplier<T> implements Supplier<T> {
    private final ArrayBlockingQueue<T> reference;
    private final Supplier<T> backingSupplier;

    public SingleSupplier(Supplier<T> backingSupplier) {
        Validate.notNull(backingSupplier);
        this.reference = new ArrayBlockingQueue<>(1);
        this.backingSupplier = backingSupplier;
    }

    @Override
    public T get() {
        T obj = backingSupplier.get();
        try {
            reference.add(obj);
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("Generated already -- only allowed to call get() once", ise);
        }
        return obj;
    }
    
    T retainedReference() throws InterruptedException {
        return reference.take();
    }
    
}
