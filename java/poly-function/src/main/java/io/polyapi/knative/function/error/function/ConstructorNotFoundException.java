package io.polyapi.knative.function.error.function;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class ConstructorNotFoundException extends PolyKNativeFunctionException {

    public ConstructorNotFoundException(Throwable cause) {
        super("Default constructor is not available on function server class.", 503, cause);
    }
}
