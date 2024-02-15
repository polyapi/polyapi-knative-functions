package io.polyapi.knative.function.error.function.state;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class PolyFunctionNotFoundException extends PolyFunctionStateException {
    public PolyFunctionNotFoundException(Throwable cause) {
        super("No uploaded class for function.", cause);
    }
}
