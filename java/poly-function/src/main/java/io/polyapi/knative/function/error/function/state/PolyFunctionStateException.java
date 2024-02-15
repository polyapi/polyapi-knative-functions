package io.polyapi.knative.function.error.function.state;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class PolyFunctionStateException extends PolyKNativeFunctionException {
    private static final Integer STATUS_CODE = 501;
    public PolyFunctionStateException(String message) {
        super(message, STATUS_CODE);
    }
    public PolyFunctionStateException(String message, Throwable cause) {
        super(message, STATUS_CODE, cause);
    }
}
