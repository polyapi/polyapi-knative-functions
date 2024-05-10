package io.polyapi.knative.function.error;

import io.polyapi.commons.api.error.PolyApiExecutionException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class PolyFunctionError {
    private final Integer statusCode;
    private final String message;
    private final Instant timestamp = Instant.now();

    public PolyFunctionError(PolyApiExecutionException exception) {
        this.statusCode = exception.getStatusCode();
        this.message = exception.getMessage();
    }
}
