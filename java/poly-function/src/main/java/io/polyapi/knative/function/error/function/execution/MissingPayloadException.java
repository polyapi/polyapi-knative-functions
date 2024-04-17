package io.polyapi.knative.function.error.function.execution;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

/**
 * Parent exception of all the exceptions that occur while executing the function.
 */
public class MissingPayloadException extends FunctionExecutionException {

    public MissingPayloadException() {
        super("Input message doesn't have a payload to process.", 400);
    }
}
