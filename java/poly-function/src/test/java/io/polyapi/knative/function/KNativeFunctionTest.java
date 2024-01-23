package io.polyapi.knative.function;

import io.polyapi.commons.api.error.PolyApiException;
import io.polyapi.commons.api.error.parse.JsonToObjectParsingException;
import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.function.WrongArgumentsException;
import io.polyapi.knative.function.mock.InnerClassParameterFunction;
import io.polyapi.knative.function.mock.MockPolyCustomFunction;
import io.polyapi.knative.function.mock.MockSingleParameterProcess;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class KNativeFunctionTest {
    private static final Logger logger = LoggerFactory.getLogger(KNativeFunctionTest.class);
    private static final String DEFAULT_SUCCESSFUL_RESULT = "";
    private static final Map<String, Object> DEFAULT_CONTENT_TYPE_HEADERS = Map.of("Content-Type", "application/json");
    private static final JsonParser jsonParser = new JacksonJsonParser();

    private static String createBody(Object... args) {
        return jsonParser.toJsonString(Map.of("args", stream(args).toList()));
    }

    public static Stream<Arguments> processTestSource() {
        return Stream.of(Arguments.of("Case 1: Empty JSon message & no headers.", new MockPolyCustomFunction(), "{}", DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Case 2: Number value to String parameter.", new MockSingleParameterProcess(param -> assertThat(param, equalTo(1))), createBody(1), DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Case 3: Default reflection generation of custom function.", null, "{}", DEFAULT_SUCCESSFUL_RESULT, null, Map.of()),
                Arguments.of("Case 4: Inner class parameter.", new InnerClassParameterFunction(), "{\"args\":[{\"name\":\"test\"}]}", "test", null, Map.of()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processTestSource")
    public void processSuccessfulTest(String caseName, Object polyCustomFunction, String payload, String expectedPayload, Map<String, Object> headers, Map<String, Object> expectedHeaders) throws NoSuchFieldException, IllegalAccessException {
        processTest(caseName, polyCustomFunction, payload, expectedPayload, headers, expectedHeaders);
    }


    public void processTest(String caseName, Object polyCustomFunction, String payload, String expectedPayload, Map<String, Object> headers, Map<String, Object> expectedHeaders) {
        logger.info(caseName);
        KNativeFunction application = Optional.ofNullable(polyCustomFunction)
                .<Supplier<Object>>map(function -> () -> function)
                .map(KNativeFunction::new)
                .orElseGet(KNativeFunction::new);
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
        return Stream.of(Arguments.of("Error case 1: Invalid JSon body.", new MockPolyCustomFunction(), "", null, JsonToObjectParsingException.class, "An error occurred while parsing JSon to io.polyapi.knative.function.model.FunctionArguments."),
                Arguments.of("Error case 2: Too many parameters.", new MockPolyCustomFunction(), createBody("test"), null, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected ()."),
                Arguments.of("Error case 3: Too few parameters.", new MockSingleParameterProcess(), createBody(), null, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.Integer)."),
                Arguments.of("Error case 4: Parameter type mismatch.", new MockSingleParameterProcess(), createBody(TRUE), null, JsonToObjectParsingException.class, "An error occurred while parsing JSon to java.lang.Integer."),
                Arguments.of("Error case 5: Exception thrown during execution of function.", new MockSingleParameterProcess(param -> {
                    throw new RuntimeException("Sample message");
                }), createBody(1), null, RuntimeException.class, "An error occurred while executing function: RuntimeException: Sample message"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("processErrorTestSource")
    public void processErrorTest(String caseName, Object polyCustomFunction, String payload, Map<String, Object> headers, Class<? extends PolyApiException> expectedException, String expectedExceptionMessage) {
        PolyApiException exception = assertThrows(expectedException, () -> processTest(caseName, polyCustomFunction, payload, "", headers, DEFAULT_CONTENT_TYPE_HEADERS));
        assertThat(exception.getMessage(), equalTo(expectedExceptionMessage));
    }
}
