package io.polyapi.knative.function.error.function;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class PolyFunctionNotFoundException extends PolyKNativeFunctionException {
    public PolyFunctionNotFoundException() {
        super("No uploaded class for function.", 503);
    }
}
