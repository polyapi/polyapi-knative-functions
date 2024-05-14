package io.polyapi.knative.function.error.function.execution;

import static java.lang.String.format;

public class PolyCustomInjectionException extends FunctionExecutionException {
    public PolyCustomInjectionException(String fieldName, IllegalAccessException cause) {
        super(format("An error occurred while setting up the PolyCustom object on field '%s'.", fieldName), 500, cause);
    }
}
