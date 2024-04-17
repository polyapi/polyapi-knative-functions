package io.polyapi.knative.function.error.function.execution;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

/**
 * Parent exception of all the exceptions that occur while executing the function.
 */
public class FunctionExecutionException extends PolyKNativeFunctionException {
    public FunctionExecutionException(String message, Integer statusCode) {
        super(message, statusCode);
    }

    public FunctionExecutionException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}
