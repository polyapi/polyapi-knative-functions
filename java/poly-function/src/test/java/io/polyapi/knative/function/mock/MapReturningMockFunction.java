package io.polyapi.knative.function.mock;

import java.util.Map;

public class MapReturningMockFunction {

    public Map<String,String> execute() {
        return Map.of("key", "value");
    }
}
