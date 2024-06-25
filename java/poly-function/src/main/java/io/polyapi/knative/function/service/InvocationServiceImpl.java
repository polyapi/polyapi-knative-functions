package io.polyapi.knative.function.service;

import io.polyapi.client.api.model.function.PolyCustom;
import io.polyapi.commons.api.error.PolyApiExecutionException;
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
import io.polyapi.knative.function.model.InvocationResult;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static io.polyapi.knative.function.log.PolyAppender.LOGGING_THREAD_PREFIX;
import static java.lang.Math.min;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Setter
@Service
public class InvocationServiceImpl implements InvocationService {
    @Value("${polyapi.function.id:}")
    private String functionId;

    @Value("${polyapi.function.api.key:}")
    private String apiKey;

    @Override
    public InvocationResult invokeFunction(Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled, String executionId) {
        try {
            CompletableFuture<Object> completableFuture = new CompletableFuture<>();
            PolyCustom polyCustom = new PolyCustom(executionId, apiKey, OK.value(), APPLICATION_JSON_VALUE);
            new Thread(() -> {
                try {
                    log.debug("Retrieving default constructor to setup the server function.");
                    Constructor<?> constructor = clazz.getDeclaredConstructor();
                    log.debug("Default constructor retrieved successfully.");
                    log.debug("Instantiating function class {} using default constructor.", clazz.getName());
                    Object function = constructor.newInstance();
                    log.debug("Class {} instantiated successfully.", clazz.getName());
                    try {
                        Stream.concat(Arrays.stream(clazz.getFields()), Arrays.stream(clazz.getDeclaredFields())).filter(field -> field.getType().equals(PolyCustom.class)).forEach(field -> {
                            try {
                                log.debug("Setting up PolyCustom on field {}.", field.getName());
                                field.setAccessible(true);
                                field.set(function, polyCustom);
                                log.debug("PolyCustom set successfully on field {}.", field.getName());
                            } catch (IllegalAccessException e) {
                                throw new PolyCustomInjectionException(field.getName(), e);
                            }
                        });
                        log.info("Executing function '{}'.", functionId);
                        Object result = method.invoke(function, Arrays.stream(arguments).toList().subList(0, min(method.getParameters().length, arguments.length)).toArray());
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
            return new InvocationResult(completableFuture.get(), polyCustom);
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