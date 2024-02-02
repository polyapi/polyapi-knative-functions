package io.polyapi.knative.function.error.function.state;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class PolyFunctionStateException extends PolyKNativeFunctionException {
    public PolyFunctionStateException(String message, Throwable cause) {
        super(message, 501, cause);
    }
}
