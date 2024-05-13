package io.polyapi.knative.function.mock.function;

import java.util.function.Consumer;

public class PrivateConstructorStringConsumer implements Consumer<String> {

    private PrivateConstructorStringConsumer() {
        // Do nothing.
    }

    @Override
    public void accept(String value) {
        // Do nothing.
    }
}
