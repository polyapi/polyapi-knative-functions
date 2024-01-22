package io.polyapi.knative.function.error.function;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

import java.lang.reflect.Method;

import static java.lang.String.format;

public class ExecuteMethodNotFoundException extends PolyKNativeFunctionException {
    public ExecuteMethodNotFoundException(Method method, Throwable cause) {
        super(format("Method '%s' is not accessible from function server class.", method.getName()), 404, cause);
    }

    public ExecuteMethodNotFoundException() {
        super("Method 'execute' is not accessible from function server class.", 404);
    }
}
