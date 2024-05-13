package io.polyapi.knative.function.mock.function;

import java.util.function.BiFunction;

public class StringIntegerToStringBiFunction implements BiFunction<String, Integer, String> {

    @Override
    public String apply(String param1, Integer param2) {
        return param1 + param2;
    }
}
