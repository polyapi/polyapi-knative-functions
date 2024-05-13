package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public abstract class AbstractStringSupplier implements Supplier<String> {

    public AbstractStringSupplier() {
        // Do nothing.
    }

    @Override
    public String get() {
        return null;
    }
}
