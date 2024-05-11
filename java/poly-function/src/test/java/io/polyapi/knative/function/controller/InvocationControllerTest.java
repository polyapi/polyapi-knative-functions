package io.polyapi.knative.function.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static java.lang.String.join;

public class InvocationControllerTest {

    public static List<Arguments> handleRequestSource() {
        return List.of(Arguments.of("", "", List.of(), ""));
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("handleRequestSource")
    public void handleRequestTest(String functionId, String functionQualifiedName, List<String> parameterTypes, String methodName) {
        InvocationController controller = new InvocationController();
        controller.setFunctionId(functionId);
        controller.setFunctionQualifiedName(functionQualifiedName);
        controller.setParameterTypes(join(",", parameterTypes));
        controller.setMethodName(methodName);
    }
}
