package io.polyapi.knative.function.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.polyapi.client.api.model.function.PolyCustom;
import io.polyapi.commons.api.error.parse.JsonToObjectParsingException;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.controller.dto.TriggerEventResult;
import io.polyapi.knative.function.error.PolyFunctionError;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.state.ExecutionMethodNotFoundException;
import io.polyapi.knative.function.error.function.state.InvalidArgumentTypeException;
import io.polyapi.knative.function.error.function.state.PolyFunctionNotFoundException;
import io.polyapi.knative.function.mock.exception.MockServiceException;
import io.polyapi.knative.function.mock.function.IntegerSupplier;
import io.polyapi.knative.function.mock.function.MockRunnable;
import io.polyapi.knative.function.mock.function.PolyCustomFunction;
import io.polyapi.knative.function.mock.function.PrivateMethodClass;
import io.polyapi.knative.function.mock.function.StatefulObject;
import io.polyapi.knative.function.mock.function.StatefulObjectConsumer;
import io.polyapi.knative.function.mock.function.StatefulObjectSupplier;
import io.polyapi.knative.function.mock.function.StringSupplier;
import io.polyapi.knative.function.mock.function.StringToStringFunction;
import io.polyapi.knative.function.model.InvocationResult;
import io.polyapi.knative.function.service.InvocationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.polyapi.knative.function.TestCaseDescriber.describeCase;
import static io.polyapi.knative.function.TestCaseDescriber.describeErrorCase;
import static io.polyapi.knative.function.mock.function.PolyCustomFunction.DEFAULT_RESULT;
import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class InvocationControllerTest {
    private static final String DEFAULT_FUNCTION_ID = "test-function-id";
    private static final String DEFAULT_EXECUTION_ID = "test-execution-id";
    private static final String DEFAULT_API_KEY = "test-execution-id";
    private static final String DEFAULT_ERROR_MESSAGE = "Error!";
    private InvocationService invocationService;
    private static final JacksonJsonParser jsonParser = new JacksonJsonParser();

    public static List<Arguments> invokeSource() {
        return List.of(createArgumentsForInvoke(1, "Invoking function with params and Object result.", StringToStringFunction.class, List.of(String.class), "apply", "Result", "Case 1"),
                createArgumentsForInvoke(2, "Invoking function with params but without result.", StatefulObjectConsumer.class, List.of(StatefulObject.class), "accept", null, new StatefulObject()),
                createArgumentsForInvoke(3, "Invoking function without params but with Object result.", StatefulObjectSupplier.class, List.of(), "get", new StatefulObject(true)),
                createArgumentsForInvoke(4, "Invoking function without params but with String result.", StringSupplier.class, List.of(), "get", "Result"),
                createArgumentsForInvoke(5, "Invoking function without params but with Integer result.", IntegerSupplier.class, List.of(), "get", 1),
                createArgumentsForInvoke(6, "Invoking function without params nor result.", MockRunnable.class, List.of(), "run", null),
                createArgumentsForInvoke(7, "Setting logsEnabled to false.", DEFAULT_FUNCTION_ID, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, MockRunnable.class.getName(), List.of(), "run", false, null),
                createArgumentsForInvoke(8, "Setting functionId to null.", null, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, MockRunnable.class.getName(), List.of(), "run", true, null),
                createArgumentsForInvoke(9, "Setting parameter types to null.", DEFAULT_FUNCTION_ID, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, PolyCustomFunction.class.getName(), null, "execute", true, DEFAULT_RESULT),
                createArgumentsForInvoke(10, "Private execution method (Delegates failure to the service layer).", PrivateMethodClass.class, List.of(), "get", null));
    }

    private static Arguments createArgumentsForInvoke(Integer caseNumber, String description, Class<?> functionClass, List<Class<?>> parameterTypes, String methodName, Object expectedBody, Object... arguments) {
        return createArgumentsForInvoke(caseNumber, description, DEFAULT_FUNCTION_ID, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, functionClass.getName(), parameterTypes.stream().map(Class::getName).toList(), methodName, true, expectedBody, Arrays.stream(arguments).map(jsonParser::toJsonString).toArray(String[]::new));
    }

    private static Arguments createArgumentsForInvoke(Integer caseNumber, String description, String functionId, String executionId, String apiKey, Integer responseStatusCode, String responseContentType, String functionQualifiedName, List<String> parameterTypes, String methodName, boolean logsEnabled, Object expectedBody, String... arguments) {

        return Arguments.of(caseNumber, description, functionId, functionQualifiedName, parameterTypes, methodName, logsEnabled, Arrays.stream(arguments).map(argument -> {
            try {
                return jsonParser.readTree(argument);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList(), new InvocationResult(expectedBody, new PolyCustom(executionId, apiKey, responseStatusCode, responseContentType)), Map.of(CONTENT_TYPE, responseContentType));
    }


    public static List<Arguments> triggerSource() {
        List<Arguments> arguments = new ArrayList<>();
        arguments.addAll(invokeSource().stream()
                .map(Arguments::get)
                .map(InvocationControllerTest::createArgumentsForTrigger)
                .toList());
        arguments.add(createArgumentsForTrigger(arguments.size() + 1, "Empty execution ID.", "", "sample", StatefulObjectConsumer.class, List.of(StatefulObject.class), "accept", null, new StatefulObject()));
        arguments.add(createArgumentsForTrigger(arguments.size() + 1, "Empty environment ID.", "sample", "", StatefulObjectConsumer.class, List.of(StatefulObject.class), "accept", null, new StatefulObject()));
        arguments.add(createArgumentsForTrigger(arguments.size() + 1, "Null execution ID.", null, "sample", StatefulObjectConsumer.class, List.of(StatefulObject.class), "accept", null, new StatefulObject()));
        arguments.add(createArgumentsForTrigger(arguments.size() + 1, "Null environment ID.", "sample", null, StatefulObjectConsumer.class, List.of(StatefulObject.class), "accept", null, new StatefulObject()));
        return arguments;
    }

    private static Arguments createArgumentsForTrigger(Object... arguments) {
        List<Object> result = Arrays.stream(arguments).collect(Collectors.toList());
        result.add(2, "sample");
        return Arguments.of(result.toArray());
    }

    private static Arguments createArgumentsForTrigger(Integer caseNumber, String description, String executionId, String environmentId, Class<?> functionClass, List<Class<?>> parameterTypes, String methodName, Object expectedBody, Object... arguments) {
        return createArgumentsForTrigger(caseNumber, description, DEFAULT_FUNCTION_ID, executionId, environmentId, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, functionClass.getName(), parameterTypes.stream().map(Class::getName).toList(), methodName, true, expectedBody, Arrays.stream(arguments).map(jsonParser::toJsonString).toArray(String[]::new));
    }

    private static Arguments createArgumentsForTrigger(Integer caseNumber, String description, String functionId, String executionId, String environmentId, String apiKey, Integer responseStatusCode, String responseContentType, String functionQualifiedName, List<String> parameterTypes, String methodName, boolean logsEnabled, Object expectedBody, String... arguments) {
        return Arguments.of(caseNumber, description, functionId, environmentId, functionQualifiedName, parameterTypes, methodName, logsEnabled, Arrays.stream(arguments).map(argument -> {
            try {
                return jsonParser.readTree(argument);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList(), new InvocationResult(expectedBody, new PolyCustom(executionId, apiKey, responseStatusCode, responseContentType)), Map.of(CONTENT_TYPE, responseContentType));
    }

    public static List<Arguments> invokeErrorSource() {
        return List.of(createArgumentsForInvokeError(1, "Function class is not present.", null, "", "", PolyFunctionNotFoundException.class, "No uploaded class for function."),
                createArgumentsForInvokeError(2, "Missing execution method.", StringToStringFunction.class, String.class.getName(), "missing", ExecutionMethodNotFoundException.class, "Method 'missing(java.lang.String)' is not accessible from function server class."),
                createArgumentsForInvokeError(3, "Wrong type of arguments.", StringToStringFunction.class, Integer.class.getName(), "apply", ExecutionMethodNotFoundException.class, "Method 'apply(java.lang.Integer)' is not accessible from function server class."),
                createArgumentsForInvokeError(4, "Too many arguments.", StringToStringFunction.class, join(",", String.class.getName(), Integer.class.getName()), "apply", ExecutionMethodNotFoundException.class, "Method 'apply(java.lang.String,java.lang.Integer)' is not accessible from function server class."),
                createArgumentsForInvokeError(5, "Too few arguments.", StringToStringFunction.class, "", "apply", ExecutionMethodNotFoundException.class, "Method 'apply()' is not accessible from function server class."),
                createArgumentsForInvokeError(6, "Non-existing argument type.", StringToStringFunction.class, "Missing", "apply", InvalidArgumentTypeException.class, "Argument of type Missing cannot be resolved by the server. Please make sure that the function is properly set."),
                createArgumentsForInvokeError(7, "Service throws exception.", StringToStringFunction.class, String.class.getName(), "apply", MockServiceException.class, MockServiceException.MESSAGE),
                createArgumentsForInvokeError(8, "No param types and no execution method..", MockRunnable.class, null, "apply", ExecutionMethodNotFoundException.class, "Method 'apply()' is not accessible from function server class."));
    }

    private static Arguments createArgumentsForInvokeError(Integer caseNumber, String description, Class<?> functionClass, String parameterTypes, String methodName, Class<? extends Throwable> expectedExceptionClass, String expectedMessage, String... arguments) {
        return Arguments.of(caseNumber, description, DEFAULT_FUNCTION_ID, DEFAULT_EXECUTION_ID, Optional.ofNullable(functionClass).map(Class::getName).orElse("Missing"), parameterTypes, methodName, true, Arrays.stream(arguments).map(argument -> {
            try {
                return jsonParser.readTree(argument);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList(), expectedExceptionClass, expectedMessage);
    }

    public static List<Arguments> handleExceptionSource() {
        return List.of(Arguments.of(1, "Default 400 exception.", new PolyKNativeFunctionException(DEFAULT_ERROR_MESSAGE, BAD_REQUEST.value())),
                Arguments.of(2, "Execution wrapped exception.", new PolyApiExecutionExceptionWrapperException(new PolyKNativeFunctionException(DEFAULT_ERROR_MESSAGE, BAD_REQUEST.value()))));
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("invokeSource")
    public void invokeTest(Integer caseNumber, String description, String functionId, String functionQualifiedName, List<String> parameterTypes, String methodName, boolean logsEnabled, List<JsonNode> arguments, InvocationResult invocationResult, Map<String, String> expectedHeaders) throws ClassNotFoundException {
        describeCase(caseNumber, description);
        InvocationController controller = new InvocationController();
        controller.setFunctionId(functionId);
        controller.setFunctionQualifiedName(functionQualifiedName);
        Optional.ofNullable(parameterTypes).map(types -> join(",", types)).ifPresent(controller::setParameterTypes);
        controller.setMethodName(methodName);
        invocationService = Mockito.mock(InvocationService.class);
        Mockito.when(invocationService.invokeFunction(eq(Class.forName(Optional.ofNullable(functionQualifiedName).orElseGet(PolyCustomFunction.class::getName))), any(), any(), eq(logsEnabled), eq(invocationResult.getMetadata().getExecutionId()))).thenReturn(invocationResult);
        controller.setInvocationService(invocationService);
        controller.setJsonParser(jsonParser);
        ResponseEntity<?> result = controller.invoke(logsEnabled, invocationResult.getMetadata().getExecutionId(), Map.of("args", arguments));
        assertThat(result.getBody(), equalTo(invocationResult.getData().orElse("")));
        assertThat(result.getHeaders().keySet(), equalTo(expectedHeaders.keySet()));
        if (expectedHeaders.containsKey(CONTENT_TYPE)) {
            Optional.ofNullable(result.getHeaders().get(CONTENT_TYPE))
                    .stream()
                    .flatMap(List::stream)
                    .forEach(header -> assertThat(header, equalTo(expectedHeaders.get(CONTENT_TYPE))));
        }
        Mockito.verify(invocationService).invokeFunction(eq(Class.forName(functionQualifiedName)), any(), any(), eq(logsEnabled), eq(invocationResult.getMetadata().getExecutionId()));
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("invokeErrorSource")
    public void invokeErrorTest(Integer caseNumber, String description, String functionId, String executionId, String functionQualifiedName, String parameterTypes, String methodName, boolean logsEnabled, List<JsonNode> arguments, Class<? extends Throwable> expectedException, String expectedMessage) throws Exception {
        describeErrorCase(caseNumber, description);
        InvocationController controller = new InvocationController();
        controller.setFunctionId(functionId);
        controller.setFunctionQualifiedName(functionQualifiedName);
        controller.setParameterTypes(parameterTypes);
        controller.setMethodName(methodName);
        invocationService = Mockito.mock(InvocationService.class);
        if (expectedException.equals(MockServiceException.class)) {
            Mockito.when(invocationService.invokeFunction(eq(Class.forName(Optional.ofNullable(functionQualifiedName).orElseGet(PolyCustomFunction.class::getName))), any(), any(), eq(logsEnabled), eq(executionId))).thenThrow(expectedException.getDeclaredConstructor().newInstance());
        }
        controller.setInvocationService(invocationService);
        controller.setJsonParser(jsonParser);
        Throwable exception = assertThrows(expectedException, () -> controller.invoke(logsEnabled, executionId, Map.of("args", arguments)));
        assertThat(exception.getMessage(), equalTo(expectedMessage));
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("triggerSource")
    public void triggerTest(Integer caseNumber, String description, String functionId, String environmentId, String functionQualifiedName, List<String> parameterTypes, String methodName, boolean logsEnabled, List<JsonNode> arguments, InvocationResult invocationResult, Map<String, String> expectedHeaders) throws ClassNotFoundException {
        describeCase(caseNumber, description);
        InvocationController controller = new InvocationController();
        controller.setFunctionId(functionId);
        controller.setFunctionQualifiedName(functionQualifiedName);
        Optional.ofNullable(parameterTypes).map(types -> join(",", types)).ifPresent(controller::setParameterTypes);
        controller.setMethodName(methodName);
        invocationService = Mockito.mock(InvocationService.class);
        Mockito.when(invocationService.invokeFunction(eq(Class.forName(Optional.ofNullable(functionQualifiedName).orElseGet(PolyCustomFunction.class::getName))), any(), any(), eq(logsEnabled), eq(invocationResult.getMetadata().getExecutionId()))).thenReturn(invocationResult);
        controller.setInvocationService(invocationService);
        controller.setJsonParser(jsonParser);
        HttpHeaders headers = new HttpHeaders();
        headers.put("sample", List.of(UUID.randomUUID().toString()));
        headers.put(CONTENT_TYPE, List.of(invocationResult.getMetadata().getResponseContentType()));
        ResponseEntity<TriggerEventResult> result = controller.trigger(headers, logsEnabled, invocationResult.getMetadata().getExecutionId(), environmentId, arguments);
        TriggerEventResult body = result.getBody();
        assertThat(body, not(nullValue()));
        assertThat(body.getContentType(), equalTo(APPLICATION_JSON_VALUE));
        assertThat(body.getEnvironmentId(), equalTo(environmentId));
        assertThat(body.getExecutionId(), equalTo(invocationResult.getMetadata().getExecutionId()));
        assertThat(body.getStatusCode(), equalTo(invocationResult.getMetadata().getResponseStatusCode()));
        assertThat(body.getFunctionId(), equalTo(functionId));
        assertThat(body.getMetrics().getStart(), notNullValue());
        assertThat(body.getMetrics().getEnd(), notNullValue());
        assertThat(body.getData(), equalTo(invocationResult.getData().orElse("")));
        Map<String, String> expectedResultingHeaders = new HashMap<>(expectedHeaders);
        expectedResultingHeaders.put("ce-type", "trigger.response");
        headers.forEach((key, value) -> expectedResultingHeaders.put(key, value.get(0)));
        assertThat(result.getHeaders().size(), equalTo(expectedResultingHeaders.size()));
        expectedResultingHeaders.forEach((key, value) -> assertThat(value, equalTo(result.getHeaders().getFirst(key))));
        Mockito.verify(invocationService).invokeFunction(eq(Class.forName(functionQualifiedName)), any(), any(), eq(logsEnabled), eq(invocationResult.getMetadata().getExecutionId()));
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("handleExceptionSource")
    public void handleExceptionTest(Integer caseNumber, String description, PolyKNativeFunctionException mockException) {
        describeErrorCase(caseNumber, description);
        InvocationController controller = new InvocationController();
        ResponseEntity<PolyFunctionError> result = controller.handleException(mockException);
        assertThat(result.getStatusCode(), equalTo(HttpStatus.valueOf(mockException.getStatusCode())));
        assertThat(result.getBody().getStatusCode(), equalTo(mockException.getStatusCode()));
        assertThat(result.getBody().getMessage(), equalTo(mockException.getMessage()));
    }

    @Test
    public void handleParsingExceptionTest() {
        InvocationController controller = new InvocationController();
        JsonToObjectParsingException mockException = Mockito.mock(JsonToObjectParsingException.class);
        Mockito.when(mockException.getMessage()).thenReturn(DEFAULT_ERROR_MESSAGE);
        ResponseEntity<PolyFunctionError> result = controller.handleParsingException(mockException);
        assertThat(result.getStatusCode(), equalTo(BAD_REQUEST));
        assertThat(result.getBody().getStatusCode(), equalTo(BAD_REQUEST.value()));
        assertThat(result.getBody().getMessage(), equalTo(DEFAULT_ERROR_MESSAGE));
        Mockito.verify(mockException, times(3)).getMessage();
    }
}
