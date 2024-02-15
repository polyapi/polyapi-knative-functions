package io.polyapi.knative.function.error.function.state;

import static java.lang.String.format;

/**
 * Exception thrown when the function class cannot be instantiated.
 */
public class ClassNotInstantiableException extends PolyFunctionStateException{
    public ClassNotInstantiableException(String className, Throwable cause) {
        super(format("Class '%s' cannot be instantiated.", className), cause);
    }
}
