package io.polyapi.knative.function.error.function.execution;

import io.polyapi.commons.api.error.PolyApiExecutionException;
import io.polyapi.knative.function.error.PolyFunctionError;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;

import java.util.Optional;

import static java.lang.String.format;

/**
 * Exception that wraps a {@link PolyApiExecutionException} thrown within the function.
 */
public class PolyApiExecutionExceptionWrapperException extends PolyKNativeFunctionException {
    public PolyApiExecutionExceptionWrapperException(PolyApiExecutionException cause) {
        super(format("An error occurred while executing function: %s: %s", Optional.ofNullable(cause.getCause()).map(Object::getClass).map(Class::getSimpleName).orElse("(No root exception)"), Optional.ofNullable(cause.getCause()).map(Throwable::getMessage).orElse("No message.")), cause.getStatusCode(), cause);
    }

    @Override
    public PolyFunctionError toErrorObject() {
        return new PolyFunctionError(PolyApiExecutionException.class.cast(getCause()));
    }
}
