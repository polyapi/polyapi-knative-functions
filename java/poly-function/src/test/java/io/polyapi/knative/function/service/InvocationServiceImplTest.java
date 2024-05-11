package io.polyapi.knative.function.service;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.ClassNotInstantiableException;
import io.polyapi.knative.function.error.function.state.ConstructorNotAccessibleException;
import io.polyapi.knative.function.error.function.state.ConstructorNotFoundException;
import io.polyapi.knative.function.error.function.state.ExecutionMethodNotAccessibleException;
import io.polyapi.knative.function.mock.function.AbstractStringSupplier;
import io.polyapi.knative.function.mock.function.ExceptionInConstructorStringSupplier;
import io.polyapi.knative.function.mock.function.MockRunnable;
import io.polyapi.knative.function.mock.function.NoDefaultConstructorStringSupplier;
import io.polyapi.knative.function.mock.function.PolyKNativeFunctionExceptionThrowingStringConsumer;
import io.polyapi.knative.function.mock.function.PrivateConstructorStringConsumer;
import io.polyapi.knative.function.mock.function.PrivateMethodClass;
import io.polyapi.knative.function.mock.function.RuntimeExceptionThrowingStringConsumer;
import io.polyapi.knative.function.mock.function.StatefulObject;
import io.polyapi.knative.function.mock.function.StatefulObjectConsumer;
import io.polyapi.knative.function.mock.function.StringIntegerToStringBiFunction;
import io.polyapi.knative.function.mock.function.StringSupplier;
import io.polyapi.knative.function.mock.function.StringToStringFunction;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;
import java.util.List;

import static io.polyapi.knative.function.mock.function.StringSupplier.DEFAULT_RESULT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@TestPropertySource("classpath:test-application.properties")
public class InvocationServiceImplTest {

    public static List<Arguments> invokeFunctionSource() throws NoSuchMethodException {
        return List.of(createArguments(1, "Function invocation.", StringToStringFunction.class, getMethod(StringToStringFunction.class,"apply", String.class), true, "1 esac", "case 1"),
                createArguments(2, "Consumer invocation.", StatefulObjectConsumer.class, getMethod(StatefulObjectConsumer.class, "accept", StatefulObject.class), true, null, new StatefulObject()),
                createArguments(3, "Bi-function invocation.", StringIntegerToStringBiFunction.class, getMethod(StringIntegerToStringBiFunction.class, "apply", String.class, Integer.class), true, "case 3", "case ", 3),
                createArguments(4, "Supplier invocation.", StringSupplier.class, getMethod(StringSupplier.class, "get"), true, DEFAULT_RESULT),
                createArguments(5, "Runnable invocation.", MockRunnable.class, getMethod(MockRunnable.class, "run"), true, null));
    }

    private static Arguments createArguments(Integer caseNumber, String description, Class<?> clazz, Method method, boolean logsEnabled, Object expectedResult, Object... arguments) {
        return Arguments.of(caseNumber, description, clazz, method, arguments, logsEnabled, expectedResult);
    }

    public static List<Arguments> invokeFunctionErrorSource() throws NoSuchMethodException {
        return List.of(createArguments(1, "Wrong method for class.", StatefulObjectConsumer.class, getMethod(StringToStringFunction.class,"apply", String.class), true, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String).", "error case 1"),
                createArguments(2, "Too few arguments for method.", StringIntegerToStringBiFunction.class, getMethod(StringIntegerToStringBiFunction.class,"apply", String.class, Integer.class), true, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String,java.lang.Integer).", "error case 2"),
                createArguments(3, "Too many arguments for method.", StringToStringFunction.class, getMethod(StringIntegerToStringBiFunction.class,"apply", String.class, Integer.class), true, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String,java.lang.Integer).", "error case 2"),
                createArguments(4, "Argument type mismatch.", StringToStringFunction.class, getMethod(StringToStringFunction.class,"apply", String.class), true, WrongArgumentsException.class, "Wrong type of arguments for poly server function. Expected (java.lang.String).", 4),
                createArguments(5, "Private default constructor.", PrivateConstructorStringConsumer.class, getMethod(PrivateConstructorStringConsumer.class,"accept", String.class), true, ConstructorNotAccessibleException.class, "Default constructor is not accessible for the application. Please review the access modifier.", "case 5"),
                createArguments(6, "No default constructor.", NoDefaultConstructorStringSupplier.class, getMethod(NoDefaultConstructorStringSupplier.class,"get"), true, ConstructorNotFoundException.class, "Default constructor is not available on function server class."),
                createArguments(7, "RuntimeException thrown within execution.", RuntimeExceptionThrowingStringConsumer.class, getMethod(RuntimeExceptionThrowingStringConsumer.class,"accept", String.class), true, UnexpectedFunctionExecutionException.class, "An unexpected exception occurred while executing the server function.", " case 7"),
                createArguments(8, "PolyKNativeFunctionException thrown within execution.", PolyKNativeFunctionExceptionThrowingStringConsumer.class, getMethod(PolyKNativeFunctionExceptionThrowingStringConsumer.class,"accept", String.class), true, PolyApiExecutionExceptionWrapperException.class, "An error occurred while executing function: (No root exception): No message.", " case 7"),
                createArguments(9, "Exception thrown in constructor.", ExceptionInConstructorStringSupplier.class, getMethod(ExceptionInConstructorStringSupplier.class,"get"), true, FunctionCreationException.class, "An error occurred while creating the server function."),
                createArguments(10, "Abstract class.", AbstractStringSupplier.class, getMethod(AbstractStringSupplier.class,"get"), true, ClassNotInstantiableException.class, "Class 'io.polyapi.knative.function.mock.function.AbstractStringSupplier' cannot be instantiated."),
                createArguments(11, "Private execution method.", PrivateMethodClass.class, getMethod(PrivateMethodClass.class,"get"), true, ExecutionMethodNotAccessibleException.class, "The execution method 'private java.lang.String io.polyapi.knative.function.mock.function.PrivateMethodClass.get()' is not accessible is not accessible for the application. Please review the access modifier."),
                createArguments(12, "Null execution method.", StringToStringFunction.class, null, true, UnexpectedFunctionExecutionException.class, "An unexpected exception occurred while executing the server function.", "case 11"));
    }

    private static Arguments createArguments(Integer caseNumber, String description, Class<?> clazz, Method method, boolean logsEnabled, Class<? extends PolyKNativeFunctionException> expectedException, String expectedErrorMessage, Object... arguments) {
        return Arguments.of(caseNumber, description, clazz, method, arguments, logsEnabled, expectedException, expectedErrorMessage);
    }

    private static Method getMethod(Class<?> clazz, String method, Class<?>... argumentTypes) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(method, argumentTypes);
    }

    @ParameterizedTest(name = "Case {0}: {1}")
    @MethodSource("invokeFunctionSource")
    public void invokeFunctionTest(Integer caseNumber, String description, Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled, Object expectedResult) {
        log.info("-------------------------- Case {} --------------------------", caseNumber);
        log.info(description);
        InvocationServiceImpl invocationService = new InvocationServiceImpl();
        invocationService.setFunctionId("Test function " + method);
        for (Object object : arguments) {
            if (object instanceof StatefulObject statefulObject) {
                assertFalse(statefulObject.isModified());
            }
        }
        assertThat(invocationService.invokeFunction(clazz, method, arguments, logsEnabled), equalTo(expectedResult));
        for (Object object : arguments) {
            if (object instanceof StatefulObject statefulObject) {
                assertTrue(statefulObject.isModified());
            }
        }
    }

    @ParameterizedTest(name = "Error case {0}: {1}")
    @MethodSource("invokeFunctionErrorSource")
    public void invokeFunctionErrorTest(Integer caseNumber, String description, Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled, Class<? extends PolyKNativeFunctionException> expectedException, String expectedErrorMessage) {
        log.info("----------------------- Error Case {} -----------------------", caseNumber);
        log.info(description);
        InvocationServiceImpl invocationService = new InvocationServiceImpl();
        invocationService.setFunctionId("Error Test function " + method);
        PolyKNativeFunctionException exception = assertThrows(expectedException, () -> invocationService.invokeFunction(clazz, method, arguments, logsEnabled));
        assertThat(exception.getMessage(), equalTo(expectedErrorMessage));
    }
}
