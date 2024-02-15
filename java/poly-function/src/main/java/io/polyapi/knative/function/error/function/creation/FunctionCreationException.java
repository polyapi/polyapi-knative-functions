package io.polyapi.knative.function.error.function.creation;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

/**
 * Exception thrown when an exception is thrown when creating the function server class.
 */
public class FunctionCreationException extends PolyKNativeFunctionException {
    public FunctionCreationException(Throwable cause) {
        super("An error occurred while creating the server function.", 500, cause);
    }
}
