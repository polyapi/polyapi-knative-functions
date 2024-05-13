package io.polyapi.knative.function;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.boot.test.context.SpringBootTest.UseMainMethod.WHEN_AVAILABLE;

@SpringBootTest(useMainMethod = WHEN_AVAILABLE)
public class KNativeFunctionTest {
    private static final Logger logger = LoggerFactory.getLogger(KNativeFunctionTest.class);
    private static final String DEFAULT_SUCCESSFUL_RESULT = "";
    private static final Map<String, Object> DEFAULT_CONTENT_TYPE_HEADERS = Map.of("Content-Type", "application/json");
    private static final Map<String, String> TRIGGER_HEADERS = Map.of("ce-id", "true", "ce-executionid", "1234", "ce-environment", "asdfg");
    private static final JsonParser jsonParser = new JacksonJsonParser();
/*
    @Autowired
    private KNativeFunction KNativeFunction;

    private static String createBody(Object... args) {
        return jsonParser.toJsonString(Map.of("args", stream(args).toList()));
    }

    private static String createParamTypes(Class... classes) {
        return Arrays.stream(classes).map(Class::getName).collect(joining(","));
    }

    public static Stream<Arguments> processTestSource() {
        return Stream.of(Arguments.of("CASE 1: Default reflection generation of custom function.", null, null, null, null, createBody("Test"), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 2: Properties file without method name property. Defaults to execute.", null, PolyCustomFunction.class.getName(), null, String.class.getName(), createBody("Test"), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 3: Properties file without parameter property. Should find any match by name.", null, PolyCustomFunction.class.getName(), "execute", null, createBody("Test"), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 4: Empty JSon message & no headers.", null, MockPolyCustomFunction.class.getName(), "execute", "", createBody(), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 5: Number value to String parameter.", null, MockSingleParameterProcess.class.getName(), "execute", Integer.class.getName(), createBody(1), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 6: Inner class parameter.", null, MockInnerClassParameterFunction.class.getName(), "execute", MockInnerClassParameterFunction.InnerClass.class.getName(), createBody(Map.of("name", "test")), null, "test", Map.of()),
                Arguments.of("CASE 7: Execution method with different name.", null, MockCustomExecutionMethodFunction.class.getName(), "differentMethod", "", createBody(), null, DEFAULT_SUCCESSFUL_RESULT, Map.of()),
                Arguments.of("CASE 8: Execution method with Map return type.", null, MapReturningMockFunction.class.getName(), "execute", "", createBody(), null, "{\"key\":\"value\"}", DEFAULT_CONTENT_TYPE_HEADERS),
                Arguments.of("CASE 9: Number returning function.", null, MockNumberReturningPolyCustomFunction.class.getName(), "execute", "", createBody(), null, 1, Map.of()),
                Arguments.of("CASE 10: Multiple params function.", null, MultipleParametersFunction.class.getName(), "execute", createParamTypes(String.class, Integer.class), createBody("Test",1), null, "Test1", Map.of()),
                Arguments.of("CASE 11: Trigger execution.", "12345678", TriggerEventFunction.class.getName(), "execute", createParamTypes(String.class, Map.class, Map.class), jsonParser.toJsonString(List.of("test", TRIGGER_HEADERS, Map.of())), TRIGGER_HEADERS, "{\"data\":\"test\",\"statusCode\":200,\"executionId\":\"1234\",\"functionId\":\"12345678\",\"environmentId\":\"asdfg\",\"contentType\":\"application/json\",\"metrics\":{\"start\":1234,\"end\":1235}}", Map.of("ce-id", "true", "ce-executionid", "1234", "ce-environment", "asdfg", "ce-type", "trigger.response")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processTestSource")
    public void processSuccessfulTest(String description, String functionId, String className, String methodName, String parameterTypes, String payload, Map<String, String> headers, Object expectedPayload, Map<String, Object> expectedHeaders) {
        processTest(description, functionId, className, methodName, parameterTypes, payload, headers, expectedPayload, expectedHeaders);
    }

    public void processTest(String description, String functionId, String className, String methodName, String parameterTypes, Object payload, Map<String, String> headers, Object expectedPayload, Map<String, Object> expectedHeaders) {
        String caseTitle = format("- %s -", description);
        String titleSeparator = range(0, caseTitle.length()).boxed().map(i -> "-").collect(joining());
        logger.info("\n{}", titleSeparator);
        logger.info(caseTitle);
        logger.info("{}\n", titleSeparator);
        Optional.ofNullable(functionId).ifPresent(KNativeFunction::setFunctionId);
        Optional.ofNullable(className).ifPresent(KNativeFunction::setFunctionQualifiedName);
        Optional.ofNullable(methodName).ifPresent(KNativeFunction::setMethodName);
        KNativeFunction.setParameterTypes(parameterTypes);
        Message<?> result = KNativeFunction.execute().apply(MessageBuilder.withPayload(payload).copyHeaders(Optional.ofNullable(headers).orElseGet(HashMap::new)).build());
        assertThat(result, notNullValue());
        assertThat(Optional.ofNullable(result.getPayload()).filter(String.class::isInstance).map(Object::toString).<Object>map(resultPayload -> resultPayload.replaceAll("\\\"start\\\":\\d*,\\\"end\\\":\\d*", "\"start\":1234,\"end\":1235")).orElse(result.getPayload()), equalTo(expectedPayload));
        Map<String, Object> returnedHeaders = new HashMap<>(result.getHeaders());
        assertThat(returnedHeaders.remove("id"), notNullValue());
        assertThat(returnedHeaders.remove("timestamp"), notNullValue());
        assertThat(returnedHeaders.size(), equalTo(expectedHeaders.size()));
        returnedHeaders.forEach((key, value) -> assertThat(value, equalTo(expectedHeaders.get(key))));
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
                Arguments.of("ERROR CASE 13: Abstract function.", AbstractFunction.class.getName(), "execute", "", createBody(), ClassNotInstantiableException.class, 501, "Class 'io.polyapi.knative.function.mock.test.AbstractFunction' cannot be instantiated."),
                Arguments.of("ERROR CASE 14: Expected exception.", MockExpectedExceptionThrowingFunction.class.getName(), "execute", "", createBody(), PolyApiExecutionExceptionWrapperException.class, 42, "An error occurred while executing function: (No root exception): No message."),
                Arguments.of("ERROR CASE 15: Private execution method.", MockPrivateExecutionMethodFunction.class.getName(), "execute", "", createBody(), ExecutionMethodNotAccessibleException.class, 501, "The execution method 'private void io.polyapi.knative.function.mock.MockPrivateExecutionMethodFunction.execute()' is not accessible is not accessible for the application. Please review the access modifier."),
                Arguments.of("ERROR CASE 16: Non existing argument type.", MockPolyCustomFunction.class.getName(), "execute", "i.dont.exist.as.AType", createBody(), InvalidArgumentTypeException.class, 501, "Argument of type i.dont.exist.as.AType cannot be resolved by the server. Please make sure that the function is properly set."));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processErrorTestSource")
    public void processErrorTest(String description, String className, String methodName, String parameterTypes, String payload, Class<? extends PolyKNativeFunctionException> expectedException, Integer expectedStatusCode, String expectedExceptionMessage) {
        PolyKNativeFunctionException exception = assertThrows(expectedException, () -> processTest(description, null, className, methodName, parameterTypes, payload, null, "", DEFAULT_CONTENT_TYPE_HEADERS));
        assertThat(exception.getMessage(), equalTo(expectedExceptionMessage));
        assertThat(exception.getStatusCode(), equalTo(expectedStatusCode));
    }

 */
}
