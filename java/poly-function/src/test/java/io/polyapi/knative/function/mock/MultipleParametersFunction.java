package io.polyapi.knative.function.mock;

public class MultipleParametersFunction {
    public String execute(String param1, Integer param2) {
        return param1 + param2;
    }
}
