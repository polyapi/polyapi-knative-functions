package io.polyapi.knative.function.error.function.state;

import static java.lang.String.format;

public class ExecutionMethodNotFoundException extends PolyFunctionStateException {
    public ExecutionMethodNotFoundException(String methodName, String parameterTypes, Throwable cause) {
        super(format("Method '%s(%s)' is not accessible from function server class.", methodName, parameterTypes), cause);
    }
}
