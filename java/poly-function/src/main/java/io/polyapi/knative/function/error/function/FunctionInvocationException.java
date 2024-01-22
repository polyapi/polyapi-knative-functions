package io.polyapi.knative.function.error.function;

import io.polyapi.commons.api.error.http.HttpResponseException;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;

import java.util.Optional;

import static java.lang.String.format;

public class FunctionInvocationException extends PolyKNativeFunctionException {
    public FunctionInvocationException(Throwable cause) {
        super(format("An error occurred while executing function: %s: %s", Optional.ofNullable(cause.getCause()).map(Object::getClass).map(Class::getSimpleName).orElse("(No root exception)"), Optional.ofNullable(cause.getCause()).map(Throwable::getMessage).orElse("No message.")), 500, cause);
    }

    public FunctionInvocationException(HttpResponseException cause) {
        super(format("A request failed during the execution of server function: %s", cause.getMessage()), cause.getResponse().statusCode(), cause);
    }

    public FunctionInvocationException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode, cause);
    }
}
