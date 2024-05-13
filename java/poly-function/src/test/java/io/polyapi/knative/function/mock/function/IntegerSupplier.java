package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public class IntegerSupplier implements Supplier<Integer> {
    public static final Integer DEFAULT_RESULT = 1;

    @Override
    public Integer get() {
        return 1;
    }
}
