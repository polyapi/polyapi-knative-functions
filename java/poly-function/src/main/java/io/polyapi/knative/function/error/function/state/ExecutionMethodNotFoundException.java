package io.polyapi.knative.function.error.function.state;

import static java.lang.String.format;

public class ExecutionMethodNotFoundException extends PolyFunctionStateException {
    private static final String MESSAGE_TEMPLATE = "Method '%s(%s)' is not accessible from function server class.";

    public ExecutionMethodNotFoundException(String methodName) {
        super(format(MESSAGE_TEMPLATE, methodName, ""));
    }

    public ExecutionMethodNotFoundException(String methodName, String parameterTypes, Throwable cause) {
        super(format(MESSAGE_TEMPLATE, methodName, parameterTypes), cause);
    }
}
