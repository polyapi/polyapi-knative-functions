package io.polyapi.knative.function.error.function.execution;

import static java.lang.String.format;

public class MissingHeaderException extends FunctionExecutionException {
    public MissingHeaderException(String header) {
        super(format("Header '%s' was expected but not found.", header), 400);
    }
}
