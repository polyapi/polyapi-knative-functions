package io.polyapi.knative.function.mock;

public class MockFixedResultFunction {

    private final Object result;

    public MockFixedResultFunction(Object result) {
        this.result = result;
    }

    public Object execute() {
        return result;
    }
}
