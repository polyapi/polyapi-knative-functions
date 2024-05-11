package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public class ExceptionInConstructorStringSupplier implements Supplier<String> {

    public ExceptionInConstructorStringSupplier() {
        throw new RuntimeException();
    }

    @Override
    public String get() {
        return null;
    }
}
