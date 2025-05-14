package io.polyapi.knative.function.error;

import io.polyapi.commons.api.error.PolyApiExecutionException;

/**
 * Wrapper class for all exceptions thrown on the execution of the Poly function.
 */
public class PolyKNativeFunctionException extends PolyApiExecutionException {

    private final int statusCode;

    public PolyKNativeFunctionException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PolyKNativeFunctionException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    public PolyFunctionError toErrorObject() {
        return new PolyFunctionError(this);
    }
}
