package io.polyapi.knative.function.error.function.execution;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.model.FunctionArguments;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class WrongArgumentsException extends FunctionExecutionException {
    public WrongArgumentsException(Method method, Throwable cause) {
        super(format("Wrong type of arguments for poly server function. Expected (%s).", Stream.of(method.getParameters()).map(Parameter::getType).map(Class::getName).collect(joining(","))), 400, cause);
    }
}
