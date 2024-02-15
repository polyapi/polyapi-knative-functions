package io.polyapi.knative.function.error.function.state;

/**
 * Exception thrown when the default constructor is not found in the function.
 */
public class ConstructorNotFoundException extends PolyFunctionStateException {

    public ConstructorNotFoundException(Throwable cause) {
        super("Default constructor is not available on function server class.", cause);
    }
}
