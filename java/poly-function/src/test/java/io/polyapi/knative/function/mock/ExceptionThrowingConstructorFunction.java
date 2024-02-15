package io.polyapi.knative.function.mock;

public class ExceptionThrowingConstructorFunction {

    public ExceptionThrowingConstructorFunction() {
        throw new RuntimeException();
    }

    public Object execute() {
        return null;
    }
}
