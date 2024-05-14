package io.polyapi.knative.function.mock.function;

import io.polyapi.client.api.model.function.PolyCustom;

import java.util.function.Consumer;

public class ErrorPolyCustomIntegerConsumer implements Consumer<Integer> {

    private final PolyCustom polyCustom = null;

    @Override
    public void accept(Integer value) {
        polyCustom.setResponseStatusCode(value);
    }
}
