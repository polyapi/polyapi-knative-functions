package io.polyapi.knative.function;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KNativeFunctionTest {
    private static final Logger logger = LoggerFactory.getLogger(KNativeFunctionTest.class);
    private static final String DEFAULT_SUCCESSFUL_RESULT = "";
    private static final Map<String, Object> DEFAULT_CONTENT_TYPE_HEADERS = Map.of("Content-Type", "application/json");
    private static final JsonParser jsonParser = new JacksonJsonParser();
    private static final Function<String, KNativeFunction> CREATE_FROM_CASE = caseName -> {
        String resourceName = format("/%s/cases/%s-%s.properties", KNativeFunctionTest.class.getPackageName().replace(".", "/"), KNativeFunctionTest.class.getSimpleName(), caseName);
        logger.info("Using case properties file '{}'.", resourceName);
        return new KNativeFunction(resourceName);
    };

    private static String createBody(Object... args) {
        return jsonParser.toJsonString(Map.of("args", stream(args).toList()));
    }

    public static Stream<Arguments> processTestSource() {
        return Stream.of(Arguments.of("Empty JSon message & no headers.", "Case 1", CREATE_FROM_CASE, createBody(), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Number value to String parameter.", "Case 2", CREATE_FROM_CASE, createBody(1), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Default reflection generation of custom function.", "Case 3", (Function<String, KNativeFunction>) caseName -> new KNativeFunction(), createBody(), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Inner class parameter.", "Case 4", CREATE_FROM_CASE, createBody(Map.of("name", "test")), "test", null, Map.of()),
                Arguments.of("Execution method with different name.", "Case 5", CREATE_FROM_CASE, createBody(), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Execution method with Map return type.", "Case 6", CREATE_FROM_CASE, createBody(), "{\"key\":\"value\"}", null, DEFAULT_CONTENT_TYPE_HEADERS),
                Arguments.of("Properties file without parameter property. Should find any match by name.", "Case 7", CREATE_FROM_CASE, createBody(), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("No Properties file.", "Case 8", CREATE_FROM_CASE, createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Properties file without method name property. Defaults to execute.", "Case 9", CREATE_FROM_CASE, createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Properties file without class name property. Defaults to PolyCustomFunction.", "Case 10", CREATE_FROM_CASE, createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()));
    }

    @ParameterizedTest(name = "{1}: {0}")
    @MethodSource("processTestSource")
    public void processSuccessfulTest(String description, String caseName, Function<String, KNativeFunction> creationStrategy, String payload, String expectedPayload, Map<String, Object> headers, Map<String, Object> expectedHeaders) throws IOException {
        processTest(description, caseName, creationStrategy, payload, expectedPayload, headers, expectedHeaders);
    }

    public void processTest(String description, String caseName, Function<String, KNativeFunction> creationStrategy, String payload, String expectedPayload, Map<String, Object> headers, Map<String, Object> expectedHeaders) throws IOException {
        String caseTitle = format("- %s: %s -", caseName.toUpperCase(), description);
        String titleSeparator = range(0, caseTitle.length()).boxed().map(i -> "-").collect(joining());
        logger.info("\n{}", titleSeparator);
        logger.info(caseTitle);
        logger.info("{}\n", titleSeparator);
        KNativeFunction application = creationStrategy.apply(caseName);
        Message<?> result = application.execute().apply(MessageBuilder.withPayload(payload).copyHeaders(headers).build());
        assertThat(result, notNullValue());
        assertThat(result.getPayload(), equalTo(expectedPayload));
        Map<String, Object> returnedHeaders = new HashMap<>(result.getHeaders());
        assertThat(returnedHeaders.remove("id"), notNullValue());
        assertThat(returnedHeaders.remove("timestamp"), notNullValue());
        assertThat(returnedHeaders.size(), equalTo(expectedHeaders.size()));
        returnedHeaders.forEach((key, value) -> {
            assertThat(value, equalTo(expectedHeaders.get(key)));
        });
    }

    public static Stream<Arguments> processErrorTestSource() {
        return Stream.of(Arguments.of("Invalid JSon body.", "Error case 1", CREATE_FROM_CASE, "", null, WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected ()."),
                Arguments.of("Too many parameters.", "Error case 2", CREATE_FROM_CASE, createBody("test"), null, WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected ()."),
                Arguments.of("Too few parameters.", "Error case 3", CREATE_FROM_CASE, createBody(), null, WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected (java.lang.Integer)."),
                Arguments.of("Parameter type mismatch.", "Error case 4", CREATE_FROM_CASE, createBody(TRUE), null, WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected (java.lang.Integer)."),
                Arguments.of("Exception thrown during execution of function.", "Error case 5", CREATE_FROM_CASE, createBody(), null, UnexpectedFunctionExecutionException.class, 500, "An unexpected exception occurred while executing the server function."),
                Arguments.of("Private constructor.", "Error case 6", CREATE_FROM_CASE, createBody(), null, ConstructorNotAccessibleException.class, 501, "Default constructor is not accessible for the application. Please review the access modifier."),
                Arguments.of("No default constructor.", "Error case 7", CREATE_FROM_CASE, createBody(), null, ConstructorNotFoundException.class, 501, "Default constructor is not available on function server class."),
                Arguments.of("Function class not found.", "Error case 8", CREATE_FROM_CASE, createBody(), null, PolyFunctionNotFoundException.class, 501, "No uploaded class for function."),
                Arguments.of("Non existing execution method.", "Error case 9", CREATE_FROM_CASE, createBody(), null, ExecutionMethodNotFoundException.class, 501, "Method 'nonExistingMethod()' is not accessible from function server class."),
                Arguments.of("Exception thrown in constructor.", "Error case 10", CREATE_FROM_CASE, createBody(), null, FunctionCreationException.class, 500, "An error occurred while creating the server function."),
                Arguments.of("Abstract function.", "Error case 11", CREATE_FROM_CASE, createBody(), null, ClassNotInstantiableException.class, 501, "Class 'io.polyapi.knative.function.mock.AbstractFunction' cannot be instantiated."),
                Arguments.of("Expected exception.", "Error case 12", CREATE_FROM_CASE, createBody(), null, PolyApiExecutionExceptionWrapperException.class, 42, "An error occurred while executing function: (No root exception): No message."),
                Arguments.of("Private execution method.", "Error case 13", CREATE_FROM_CASE, createBody(), null, ExecutionMethodNotAccessibleException.class, 501, "The execution method 'private void io.polyapi.knative.function.mock.MockPrivateExecutionMethodFunction.execute()' is not accessible is not accessible for the application. Please review the access modifier."),
                Arguments.of("Non existing argument type.", "Error case 14", CREATE_FROM_CASE, createBody(), null, InvalidArgumentTypeException.class, 501, "Argument of type i.dont.exist.as.AType cannot be resolved by the server. Please make sure that the function is properly set."),
                Arguments.of("Properties file with malformed unicode escape character.", "Error case 16", CREATE_FROM_CASE, createBody(), null, InvalidConfigurationPropertiesFileException.class, 501, "File '/io/polyapi/knative/function/cases/KNativeFunctionTest-Error case 16.properties' containing the configuration of the server function is invalid."));
    }

    @ParameterizedTest(name = "{1}: {0}")
    @MethodSource("processErrorTestSource")
    public void processErrorTest(String description, String caseName, Function<String, KNativeFunction> creationStrategy, String payload, Map<String, Object> headers, Class<? extends PolyKNativeFunctionException> expectedException, Integer expectedStatusCode, String expectedExceptionMessage) {
        PolyKNativeFunctionException exception = assertThrows(expectedException, () -> processTest(description, caseName, creationStrategy, payload, "", headers, DEFAULT_CONTENT_TYPE_HEADERS));
        assertThat(exception.getMessage(), equalTo(expectedExceptionMessage));
        assertThat(exception.getStatusCode(), equalTo(expectedStatusCode));
    }
}
