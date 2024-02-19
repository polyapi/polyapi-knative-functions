package io.polyapi.knative.function;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.*;
import io.polyapi.knative.function.mock.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import static org.springframework.boot.test.context.SpringBootTest.UseMainMethod.WHEN_AVAILABLE;

@SpringBootTest(useMainMethod = WHEN_AVAILABLE)
public class KNativeFunctionTest {
    private static final Logger logger = LoggerFactory.getLogger(KNativeFunctionTest.class);
    private static final String DEFAULT_SUCCESSFUL_RESULT = "";
    private static final Map<String, Object> DEFAULT_CONTENT_TYPE_HEADERS = Map.of("Content-Type", "application/json");
    private static final JsonParser jsonParser = new JacksonJsonParser();

    @Autowired
    private KNativeFunction kNativeFunction;

    private static String createBody(Object... args) {
        return jsonParser.toJsonString(Map.of("args", stream(args).toList()));
    }

    public static Stream<Arguments> processTestSource() {
        return Stream.of(Arguments.of("CASE 1: Default reflection generation of custom function.", null, null, null, createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 2: Properties file without method name property. Defaults to execute.", PolyCustomFunction.class.getName(), null, String.class.getName(), createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 3: Properties file without parameter property. Should find any match by name.", PolyCustomFunction.class.getName(), "execute", null, createBody("Test"), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 4: Empty JSon message & no headers.", MockPolyCustomFunction.class.getName(), "execute", "", createBody(), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 5: Number value to String parameter.", MockSingleParameterProcess.class.getName(), "execute", Integer.class.getName(), createBody(1), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 6: Inner class parameter.", MockInnerClassParameterFunction.class.getName(), "execute", MockInnerClassParameterFunction.InnerClass.class.getName(), createBody(Map.of("name", "test")), "test", Map.of()),
                Arguments.of("CASE 7: Execution method with different name.", MockCustomExecutionMethodFunction.class.getName(), "differentMethod", "", createBody(), DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 8: Execution method with Map return type.", MapReturningMockFunction.class.getName(), "execute", "", createBody(), "{\"key\":\"value\"}", DEFAULT_CONTENT_TYPE_HEADERS),
                Arguments.of("CASE 9: Number returning function.", MockNumberReturningPolyCustomFunction.class.getName(), "execute", "", createBody(), 1, Map.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processTestSource")
    public void processSuccessfulTest(String description, String className, String methodName, String parameterTypes, String payload, Object expectedPayload, Map<String, Object> expectedHeaders) {
        processTest(description, className, methodName, parameterTypes, payload, expectedPayload, expectedHeaders);
    }

    public void processTest(String description, String className, String methodName, String parameterTypes, String payload, Object expectedPayload, Map<String, Object> expectedHeaders) {
        String caseTitle = format("- %s -", description);
        String titleSeparator = range(0, caseTitle.length()).boxed().map(i -> "-").collect(joining());
        logger.info("\n{}", titleSeparator);
        logger.info(caseTitle);
        logger.info("{}\n", titleSeparator);
        Optional.ofNullable(className).ifPresent(kNativeFunction::setFunctionQualifiedName);
        Optional.ofNullable(methodName).ifPresent(kNativeFunction::setMethodName);
        kNativeFunction.setParameterTypes(parameterTypes);
        Message<?> result = kNativeFunction.execute().apply(MessageBuilder.withPayload(payload).build());
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
        return Stream.of(Arguments.of("ERROR CASE 1: Default class name and not set parameter types with non existing method name.", null, "nonExistingMethod", null, createBody(), ExecutionMethodNotFoundException.class, 501, "Method 'nonExistingMethod()' is not accessible from function server class."),
                Arguments.of("ERROR CASE 2: Default class name with non existing method name.", null, "nonExistingMethod", "", createBody(), ExecutionMethodNotFoundException.class, 501, "Method 'nonExistingMethod()' is not accessible from function server class."),
                Arguments.of("ERROR CASE 3: Invalid JSon body.", MockPolyCustomFunction.class.getName(), "execute", "", "", WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected ()."),
                Arguments.of("ERROR CASE 4: Too many parameters.", MockPolyCustomFunction.class.getName(), "execute", "", createBody("test"), WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected ()."),
                Arguments.of("ERROR CASE 5: Too few parameters.", MockSingleParameterProcess.class.getName(), "execute", Integer.class.getName(), createBody(), WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected (java.lang.Integer)."),
                Arguments.of("ERROR CASE 6: Parameter type mismatch.", MockSingleParameterProcess.class.getName(), "execute", Integer.class.getName(), createBody(TRUE), WrongArgumentsException.class, 400, "Wrong type of arguments for poly server function. Expected (java.lang.Integer)."),
                Arguments.of("ERROR CASE 7: Exception thrown during execution of function.", MockRuntimeExceptionThrowingFunction.class.getName(), "execute", "", createBody(), UnexpectedFunctionExecutionException.class, 500, "An unexpected exception occurred while executing the server function."),
                Arguments.of("ERROR CASE 8: Private constructor.", PrivateConstructorFunction.class.getName(), "execute", "", createBody(), ConstructorNotAccessibleException.class, 501, "Default constructor is not accessible for the application. Please review the access modifier."),
                Arguments.of("ERROR CASE 9: No default constructor.", ConstructorWithArgumentsFunction.class.getName(), "execute", "", createBody(), ConstructorNotFoundException.class, 501, "Default constructor is not available on function server class."),
                Arguments.of("ERROR CASE 10: Function class not found.", "NonExistingClass", "execute", "", createBody(), PolyFunctionNotFoundException.class, 501, "No uploaded class for function."),
                Arguments.of("ERROR CASE 11: Non existing execution method.", MockRuntimeExceptionThrowingFunction.class.getName(), "nonExistingMethod", "", createBody(), ExecutionMethodNotFoundException.class, 501, "Method 'nonExistingMethod()' is not accessible from function server class."),
                Arguments.of("ERROR CASE 12: Exception thrown in constructor.", ExceptionThrowingConstructorFunction.class.getName(), "execute", "", createBody(), FunctionCreationException.class, 500, "An error occurred while creating the server function."),
                Arguments.of("ERROR CASE 13: Abstract function.", AbstractFunction.class.getName(), "execute", "", createBody(), ClassNotInstantiableException.class, 501, "Class 'io.polyapi.knative.function.mock.AbstractFunction' cannot be instantiated."),
                Arguments.of("ERROR CASE 14: Expected exception.", MockExpectedExceptionThrowingFunction.class.getName(), "execute", "", createBody(), PolyApiExecutionExceptionWrapperException.class, 42, "An error occurred while executing function: (No root exception): No message."),
                Arguments.of("ERROR CASE 15: Private execution method.", MockPrivateExecutionMethodFunction.class.getName(), "execute", "", createBody(), ExecutionMethodNotAccessibleException.class, 501, "The execution method 'private void io.polyapi.knative.function.mock.MockPrivateExecutionMethodFunction.execute()' is not accessible is not accessible for the application. Please review the access modifier."),
                Arguments.of("ERROR CASE 16: Non existing argument type.", MockPolyCustomFunction.class.getName(), "execute", "i.dont.exist.as.AType", createBody(), InvalidArgumentTypeException.class, 501, "Argument of type i.dont.exist.as.AType cannot be resolved by the server. Please make sure that the function is properly set."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processErrorTestSource")
    public void processErrorTest(String description, String className, String methodName, String parameterTypes, String payload, Class<? extends PolyKNativeFunctionException> expectedException, Integer expectedStatusCode, String expectedExceptionMessage) {
        PolyKNativeFunctionException exception = assertThrows(expectedException, () -> processTest(description, className, methodName, parameterTypes, payload, "", DEFAULT_CONTENT_TYPE_HEADERS));
        assertThat(exception.getMessage(), equalTo(expectedExceptionMessage));
        assertThat(exception.getStatusCode(), equalTo(expectedStatusCode));
    }
}
