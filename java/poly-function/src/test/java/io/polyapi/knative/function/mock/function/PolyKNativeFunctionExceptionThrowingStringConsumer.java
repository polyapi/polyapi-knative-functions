package io.polyapi.knative.function.mock.function;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

import java.util.function.Consumer;

public class PolyKNativeFunctionExceptionThrowingStringConsumer implements Consumer<String> {
    @Override
    public void accept(String value) {
        throw new PolyKNativeFunctionException("Sample message", 400);
    }
}
