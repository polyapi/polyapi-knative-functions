package io.polyapi.knative.function.error;

import io.polyapi.commons.api.error.PolyApiException;
import io.polyapi.commons.api.error.PolyApiExecutionException;
import lombok.Getter;

/**
 * Wrapper class for all exceptions thrown on the execution of the Poly function.
 */
@Getter
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

    public PolyFunctionError toErrorObject() {
        return new PolyFunctionError(this);
    }
}
