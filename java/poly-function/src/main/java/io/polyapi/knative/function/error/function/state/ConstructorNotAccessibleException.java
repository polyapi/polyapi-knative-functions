package io.polyapi.knative.function.error.function.state;

/**
 * Exception thrown when the default constructor of the function is not accessible.
 */
public class ConstructorNotAccessibleException extends PolyFunctionStateException {
    public ConstructorNotAccessibleException(Throwable cause) {
        super("Default constructor is not accessible for the application. Please review the access modifier.", cause);
    }
}
