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
