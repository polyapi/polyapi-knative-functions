package io.polyapi.knative.function.mock;

public class ConstructorWithArgumentsFunction {

    private final Object result;

    public ConstructorWithArgumentsFunction(Object result) {
        this.result = result;
    }

    public Object execute() {
        return result;
    }
}
