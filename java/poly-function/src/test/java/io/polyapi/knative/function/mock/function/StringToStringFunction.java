package io.polyapi.knative.function.mock.function;

import java.util.function.Function;

public class StringToStringFunction implements Function<String, String> {

    @Override
    public String apply(String param1) {
        return new StringBuilder(param1).reverse().toString();
    }
}
