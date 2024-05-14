package io.polyapi.knative.function.service;

import io.polyapi.client.api.model.function.PolyCustom;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.PolyCustomInjectionException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.ClassNotInstantiableException;
import io.polyapi.knative.function.error.function.state.ConstructorNotAccessibleException;
import io.polyapi.knative.function.error.function.state.ConstructorNotFoundException;
import io.polyapi.knative.function.error.function.state.ExecutionMethodNotAccessibleException;
import io.polyapi.knative.function.mock.function.AbstractStringSupplier;
import io.polyapi.knative.function.mock.function.BiPolyCustomIntegerBiConsumer;
import io.polyapi.knative.function.mock.function.ErrorPolyCustomIntegerConsumer;
import io.polyapi.knative.function.mock.function.ExceptionInConstructorStringSupplier;
import io.polyapi.knative.function.mock.function.MockRunnable;
import io.polyapi.knative.function.mock.function.NoDefaultConstructorStringSupplier;
import io.polyapi.knative.function.mock.function.PolyCustomIntegerConsumer;
import io.polyapi.knative.function.mock.function.PolyKNativeFunctionExceptionThrowingStringConsumer;
import io.polyapi.knative.function.mock.function.PrivateConstructorStringConsumer;
import io.polyapi.knative.function.mock.function.PrivateMethodClass;
import io.polyapi.knative.function.mock.function.RuntimeExceptionThrowingStringConsumer;
import io.polyapi.knative.function.mock.function.StatefulObject;
import io.polyapi.knative.function.mock.function.StatefulObjectConsumer;
import io.polyapi.knative.function.mock.function.StringIntegerToStringBiFunction;
import io.polyapi.knative.function.mock.function.StringSupplier;
import io.polyapi.knative.function.mock.function.StringToStringFunction;
import io.polyapi.knative.function.model.InvocationResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static io.polyapi.knative.function.TestCaseDescriber.describeCase;
import static io.polyapi.knative.function.TestCaseDescriber.describeErrorCase;
import static io.polyapi.knative.function.mock.function.StringSupplier.DEFAULT_RESULT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Slf4j
@TestPropertySource("classpath:test-application.properties")
public class InvocationServiceImplTest {
    private static final String DEFAULT_EXECUTION_ID = UUID.randomUUID().toString();
    private static final String DEFAULT_API_KEY = UUID.randomUUID().toString();

    public static List<Arguments> invokeFunctionSource() throws NoSuchMethodException {
        return List.of(createArguments(1, "Function invocation.", StringToStringFunction.class, getMethod(StringToStringFunction.class,"apply", String.class), true, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, "1 esac", "case 1"),
                createArguments(2, "Consumer invocation.", StatefulObjectConsumer.class, getMethod(StatefulObjectConsumer.class, "accept", StatefulObject.class), true, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, null, new StatefulObject()),
                createArguments(3, "Bi-function invocation.", StringIntegerToStringBiFunction.class, getMethod(StringIntegerToStringBiFunction.class, "apply", String.class, Integer.class), true, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, "case 3", "case ", 3),
                createArguments(4, "Supplier invocation.", StringSupplier.class, getMethod(StringSupplier.class, "get"), true, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, DEFAULT_RESULT),
                createArguments(5, "Runnable invocation.", MockRunnable.class, getMethod(MockRunnable.class, "run"), true, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, null),
                createArguments(6, "Poly logs disabled.", StringToStringFunction.class, getMethod(StringToStringFunction.class,"apply", String.class), false, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, OK.value(), APPLICATION_JSON_VALUE, "6 esac", "case 6"),
                createArguments(7, "Poly custom function.", PolyCustomIntegerConsumer.class, getMethod(PolyCustomIntegerConsumer.class,"accept", Integer.class), false, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, 500, APPLICATION_JSON_VALUE, null, 500),
                createArguments(8, "Function with 2 Poly custom objects.", BiPolyCustomIntegerBiConsumer.class, getMethod(BiPolyCustomIntegerBiConsumer.class,"accept", Integer.class, String.class), false, DEFAULT_EXECUTION_ID, DEFAULT_API_KEY, 500, APPLICATION_XML_VALUE, null, 500, APPLICATION_XML_VALUE));
    }

    private static Arguments createArguments(Integer caseNumber, String description, Class<?> clazz, Method method, boolean logsEnabled, String executionId, String apiKey, Integer responseStatusCode, String responseContentType, Object expectedResult, Object... arguments) {
        return Arguments.of(caseNumber, description, clazz, method, arguments, logsEnabled, new InvocationResult(expectedResult, new PolyCustom(executionId, apiKey, responseStatusCode, responseContentType)));
    }

    public static List<Arguments> invokeFunctionErrorSource() throws NoSuchMethodException {
        return List.of(createArguments(1, "Wrong method for class.", StatefulObjectConsumer.class, getMethod(StringToStringFunction.class,"apply", String.class), WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String).", "error case 1"),
                createArguments(2, "Too few arguments for method.", StringIntegerToStringBiFunction.class, getMethod(StringIntegerToStringBiFunction.class,"apply", String.class, Integer.class), WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String,java.lang.Integer).", "error case 2"),
                createArguments(3, "Too many arguments for method.", StringToStringFunction.class, getMethod(StringIntegerToStringBiFunction.class,"apply", String.class, Integer.class), WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String,java.lang.Integer).", "error case 2"),
                createArguments(4, "Argument type mismatch.", StringToStringFunction.class, getMethod(StringToStringFunction.class,"apply", String.class), WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String).", 4),
                createArguments(5, "Private default constructor.", PrivateConstructorStringConsumer.class, getMethod(PrivateConstructorStringConsumer.class,"accept", String.class), ConstructorNotAccessibleException.class, "Default constructor is not accessible for the application. Please review the access modifier.", "case 5"),
                createArguments(6, "No default constructor.", NoDefaultConstructorStringSupplier.class, getMethod(NoDefaultConstructorStringSupplier.class,"get"), ConstructorNotFoundException.class, "Default constructor is not available on function server class."),
                createArguments(7, "RuntimeException thrown within execution.", RuntimeExceptionThrowingStringConsumer.class, getMethod(RuntimeExceptionThrowingStringConsumer.class,"accept", String.class), UnexpectedFunctionExecutionException.class, "An unexpected exception occurred while executing the server function.", " case 7"),
                createArguments(8, "PolyKNativeFunctionException thrown within execution.", PolyKNativeFunctionExceptionThrowingStringConsumer.class, getMethod(PolyKNativeFunctionExceptionThrowingStringConsumer.class,"accept", String.class), PolyApiExecutionExceptionWrapperException.class, "An error occurred while executing function: (No root exception): No message.", " case 7"),
                createArguments(9, "Exception thrown in constructor.", ExceptionInConstructorStringSupplier.class, getMethod(ExceptionInConstructorStringSupplier.class,"get"), FunctionCreationException.class, "An error occurred while creating the server function."),
                createArguments(10, "Abstract class.", AbstractStringSupplier.class, getMethod(AbstractStringSupplier.class,"get"), ClassNotInstantiableException.class, "Class 'io.polyapi.knative.function.mock.function.AbstractStringSupplier' cannot be instantiated."),
                createArguments(11, "Private execution method.", PrivateMethodClass.class, getMethod(PrivateMethodClass.class,"get"), ExecutionMethodNotAccessibleException.class, "The execution method 'private java.lang.String io.polyapi.knative.function.mock.function.PrivateMethodClass.get()' is not accessible is not accessible for the application. Please review the access modifier."),
                createArguments(12, "Null execution method.", StringToStringFunction.class, null, UnexpectedFunctionExecutionException.class, "An unexpected exception occurred while executing the server function.", "case 12"),
                createArguments(13, "Final PolyCustom field.", ErrorPolyCustomIntegerConsumer.class, null, UnexpectedFunctionExecutionException.class, "An unexpected exception occurred while executing the server function.", 13));
    }

    private static Arguments createArguments(Integer caseNumber, String description, Class<?> clazz, Method method, Class<? extends PolyKNativeFunctionException> expectedException, String expectedErrorMessage, Object... arguments) {
        return Arguments.of(caseNumber, description, clazz, method, arguments, expectedException, expectedErrorMessage);
    }

    private static Method getMethod(Class<?> clazz, String method, Class<?>... argumentTypes) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(method, argumentTypes);
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("invokeFunctionSource")
    public void invokeFunctionTest(Integer caseNumber, String description, Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled, InvocationResult expectedResult) {
        describeCase(caseNumber, description);
        InvocationServiceImpl invocationService = new InvocationServiceImpl();
        invocationService.setFunctionId("Test function " + method);
        invocationService.setApiKey(expectedResult.getMetadata().getExecutionApiKey());
        for (Object object : arguments) {
            if (object instanceof StatefulObject statefulObject) {
                assertFalse(statefulObject.isModified());
            }
        }
        assertThat(invocationService.invokeFunction(clazz, method, arguments, logsEnabled, expectedResult.getMetadata().getExecutionId()), equalTo(expectedResult));
        for (Object object : arguments) {
            if (object instanceof StatefulObject statefulObject) {
                assertTrue(statefulObject.isModified());
            }
        }
    }

    @ParameterizedTest(name = "Error case {0}: {1}")
    @MethodSource("invokeFunctionErrorSource")
    public void invokeFunctionErrorTest(Integer caseNumber, String description, Class<?> clazz, Method method, Object[] arguments, Class<? extends PolyKNativeFunctionException> expectedException, String expectedErrorMessage) {
        describeErrorCase(caseNumber, description);
        InvocationServiceImpl invocationService = new InvocationServiceImpl();
        invocationService.setFunctionId("Error Test function " + method);
        PolyKNativeFunctionException exception = assertThrows(expectedException, () -> invocationService.invokeFunction(clazz, method, arguments, true, UUID.randomUUID().toString()));
        assertThat(exception.getMessage(), equalTo(expectedErrorMessage));
    }
}
