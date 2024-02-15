package io.polyapi.knative.function.error.function.state;

import io.polyapi.knative.function.error.function.state.PolyFunctionStateException;
import lombok.Getter;

import static java.lang.String.format;

@Getter
public class InvalidArgumentTypeException extends PolyFunctionStateException {

    public InvalidArgumentTypeException(String typeQualifiedName, Throwable cause) {
        super(format("Argument of type %s cannot be resolved by the server. Please make sure that the function is properly set.", typeQualifiedName), cause);
    }
}
