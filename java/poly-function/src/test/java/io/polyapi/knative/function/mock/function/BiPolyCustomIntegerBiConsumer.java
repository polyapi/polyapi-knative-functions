package io.polyapi.knative.function.mock.function;

import io.polyapi.client.api.model.function.PolyCustom;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BiPolyCustomIntegerBiConsumer implements BiConsumer<Integer, String> {

    private PolyCustom polyCustom;
    private PolyCustom polyCustom2;

    @Override
    public void accept(Integer value, String value2) {
        polyCustom.setResponseStatusCode(value);
        polyCustom2.setResponseContentType(value2);
    }
}
