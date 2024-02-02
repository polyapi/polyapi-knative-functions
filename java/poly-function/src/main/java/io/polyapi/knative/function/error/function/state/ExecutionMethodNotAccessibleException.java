package io.polyapi.knative.function.error.function.state;

import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * Exception thrown when the execution method of the function is not accessible.
 */
public class ExecutionMethodNotAccessibleException extends PolyFunctionStateException {
    public ExecutionMethodNotAccessibleException(Method method, Throwable cause) {
        super(format("The execution method '%s' is not accessible is not accessible for the application. Please review the access modifier.", method), cause);
    }
}
