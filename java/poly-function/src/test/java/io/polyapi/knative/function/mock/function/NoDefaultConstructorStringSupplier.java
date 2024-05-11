package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public class NoDefaultConstructorStringSupplier implements Supplier<String> {

    private final String value;

    public NoDefaultConstructorStringSupplier(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }
}
