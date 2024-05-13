package io.polyapi.knative.function.mock.function;

import java.util.function.Consumer;

public class RuntimeExceptionThrowingStringConsumer implements Consumer<String> {
    @Override
    public void accept(String value) {
        throw new RuntimeException();
    }
}
