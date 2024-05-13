package io.polyapi.knative.function.service;

import io.polyapi.commons.api.error.PolyApiExecutionException;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.ClassNotInstantiableException;
import io.polyapi.knative.function.error.function.state.ConstructorNotAccessibleException;
import io.polyapi.knative.function.error.function.state.ConstructorNotFoundException;
import io.polyapi.knative.function.error.function.state.ExecutionMethodNotAccessibleException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.polyapi.knative.function.log.PolyAppender.LOGGING_THREAD_PREFIX;
import static java.lang.String.format;

@Slf4j
@Setter
@Service
public class InvocationServiceImpl implements InvocationService {
    @Value("${polyapi.function.id:}")
    private String functionId;

    @Override
    public Object invokeFunction(Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled) {
        try {
            CompletableFuture<Object> completableFuture = new CompletableFuture<>();
            new Thread(() -> {
                try {
                    log.debug("Retrieving default constructor to setup the server function.");
                    Constructor<?> constructor = clazz.getDeclaredConstructor();
                    log.debug("Default constructor retrieved successfully.");
                    log.debug("Instantiating function class {} using default constructor.", clazz.getName());
                    Object function = constructor.newInstance();
                    log.debug("Class {} instantiated successfully.", clazz.getName());
                    try {
                        log.info("Executing function '{}'.", functionId);
                        Object result = method.invoke(function, arguments);
                        log.info("Function '{}' executed successfully.", functionId);
                        completableFuture.complete(result);
                    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                        completableFuture.completeExceptionally(new WrongArgumentsException(method, e));
                    } catch (IllegalAccessException e) {
                        completableFuture.completeExceptionally(new ExecutionMethodNotAccessibleException(method, e));
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof PolyApiExecutionException expectedException) {
                            completableFuture.completeExceptionally(new PolyApiExecutionExceptionWrapperException(expectedException));
                        } else {
                            completableFuture.completeExceptionally(new UnexpectedFunctionExecutionException(e.getCause()));
                        }
                    }
                } catch (InvocationTargetException e) {
                    completableFuture.completeExceptionally(new FunctionCreationException(e));
                } catch (IllegalAccessException e) {
                    completableFuture.completeExceptionally(new ConstructorNotAccessibleException(e));
                } catch (InstantiationException e) {
                    completableFuture.completeExceptionally(new ClassNotInstantiableException(clazz.getName(), e));
                } catch (NoSuchMethodException e) {
                    completableFuture.completeExceptionally(new ConstructorNotFoundException(e));
                } catch (RuntimeException e) {
                    completableFuture.completeExceptionally(e);
                }
            },
                    format("%sThread-%s", logsEnabled ? LOGGING_THREAD_PREFIX : "", UUID.randomUUID())).start();
            return completableFuture.get();
        } catch (InterruptedException e) {
            throw new UnexpectedFunctionExecutionException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PolyKNativeFunctionException cause) {
                throw cause;
            } else {
                throw new UnexpectedFunctionExecutionException(e);
            }
        }
    }
}