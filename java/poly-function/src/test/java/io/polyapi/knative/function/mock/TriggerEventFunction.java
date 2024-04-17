package io.polyapi.knative.function.mock;

import java.util.Map;

public class TriggerEventFunction {

    public String execute(String message, Map<String, String> headers, Map<String, Object> params) {
        return message;
    }
}
