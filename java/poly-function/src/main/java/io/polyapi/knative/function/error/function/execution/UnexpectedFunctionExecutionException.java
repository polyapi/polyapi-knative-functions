package io.polyapi.knative.function.error.function.execution;

public class UnexpectedFunctionExecutionException extends FunctionExecutionException {
    public UnexpectedFunctionExecutionException(Throwable cause) {
        super("An unexpected exception occurred while executing the server function.", 500, cause);
    }
}
