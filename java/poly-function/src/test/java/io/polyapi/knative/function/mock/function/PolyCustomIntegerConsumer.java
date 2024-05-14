package io.polyapi.knative.function.mock.function;

import io.polyapi.client.api.model.function.PolyCustom;

import java.util.function.Consumer;

public class PolyCustomIntegerConsumer implements Consumer<Integer> {

    private PolyCustom polyCustom;

    @Override
    public void accept(Integer value) {
        polyCustom.setResponseStatusCode(value);
    }
}
