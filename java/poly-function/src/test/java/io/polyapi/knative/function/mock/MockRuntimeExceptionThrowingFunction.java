package io.polyapi.knative.function.mock;

public class MockRuntimeExceptionThrowingFunction {

    public void execute() {
        throw new RuntimeException("Sample message");
    }
}
