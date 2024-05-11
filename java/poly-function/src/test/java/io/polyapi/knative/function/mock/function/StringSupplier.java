package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public class StringSupplier implements Supplier<String> {
    public static final String DEFAULT_RESULT = "Hello there!";

    @Override
    public String get() {
        return DEFAULT_RESULT;
    }
}
