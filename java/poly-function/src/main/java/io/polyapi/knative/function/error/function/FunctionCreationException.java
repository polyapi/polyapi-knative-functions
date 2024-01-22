package io.polyapi.knative.function.error.function;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class FunctionCreationException extends PolyKNativeFunctionException {
    public FunctionCreationException(Throwable cause) {
        super("An error occurred while initiating the server function object.", 500, cause);
    }
}
