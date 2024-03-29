package io.polyapi.knative.function;

import io.polyapi.commons.api.error.PolyApiExecutionException;
import io.polyapi.commons.api.error.parse.JsonToObjectParsingException;
import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.*;
import io.polyapi.knative.function.log.PolyAppender;
import io.polyapi.knative.function.model.FunctionArguments;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static io.polyapi.knative.function.log.PolyAppender.LOGGING_THREAD_PREFIX;
import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.function.Predicate.not;
import static java.util.stream.IntStream.range;

@SpringBootApplication
@Setter
@Slf4j
public class KNativeFunction {

    private final JsonParser jsonParser = new JacksonJsonParser();

    @Value("${polyapi.function.class:io.polyapi.knative.function.PolyCustomFunction}")
    private String functionQualifiedName;

    @Value("${polyapi.function.method:execute}")
    private String methodName;

    @Value("${polyapi.function.params:#{null}}")
    private String parameterTypes;

    public static void main(String[] args) {
        SpringApplication.run(KNativeFunction.class, args);
    }

    @Bean
    public Function<Message<String>, Message<?>> execute() {
        return this::process;
    }

    private Message<?> process(Message<String> inputMessage) {
        try {
            Thread.currentThread().setName(LOGGING_THREAD_PREFIX.concat(Thread.currentThread().getName()));
            boolean loggingEnabled = Optional.ofNullable(inputMessage.getHeaders().get("x-poly-do-log"))
                    .map(Object::toString)
                    .map(Boolean::parseBoolean)
                    .orElse(FALSE);
            log.info("Loading class {}.", functionQualifiedName);
            Class<?> functionClass = Class.forName(functionQualifiedName);
            log.debug("Class {} loaded successfully.", functionQualifiedName);
            try {
                Method functionMethod;
                Class<?>[] paramTypes;
                if (parameterTypes == null) {
                    functionMethod = Arrays.stream(functionClass.getDeclaredMethods()).filter(method -> method.getName().equals(methodName)).findFirst().orElseThrow(() -> new ExecutionMethodNotFoundException(methodName));
                } else {
                    try {
                        log.info("Loading parameter types: [{}].", parameterTypes);
                        paramTypes = Optional.of(parameterTypes)
                                .filter(not(String::isBlank))
                                .map(params -> params.split(","))
                                .stream()
                                .flatMap(Arrays::stream)
                                .map(String::trim)
                                .map(qualifiedName -> {
                                    try {
                                        log.debug("Loading class for parameter type '{}'.", qualifiedName);
                                        Class<?> result = Class.forName(qualifiedName);
                                        log.debug("Class loaded successfully.");
                                        return result;
                                    } catch (ClassNotFoundException e) {
                                        throw new InvalidArgumentTypeException(qualifiedName, e);
                                    }
                                })
                                .toArray(Class<?>[]::new);
                        log.debug("Parameter types loaded successfully.");
                        log.info("Retrieving method {}.{}({}).", functionQualifiedName, methodName, parameterTypes);
                        functionMethod = functionClass.getDeclaredMethod(methodName, paramTypes);
                    } catch (NoSuchMethodException e) {
                        throw new ExecutionMethodNotFoundException(methodName, parameterTypes, e);
                    }

                }
                log.debug("Method {} retrieved successfully.", functionMethod);
                log.debug("Executing function with payload {}.", inputMessage.getPayload());
                log.info("Retrieving default constructor to setup the server function.");
                Constructor<?> constructor = functionClass.getDeclaredConstructor();
                log.debug("Default constructor retrieved successfully.");
                log.info("Instantiating function class {} using default constructor.", functionQualifiedName);
                Object function = constructor.newInstance();
                log.info("Class {} instantiated successfully.", functionQualifiedName);
                try {
                    log.debug("Parsing payload.");
                    FunctionArguments arguments = jsonParser.parseString(inputMessage.getPayload(), FunctionArguments.class);
                    log.debug("Parse successful.");
                    log.info("Executing function.");
                    CompletableFuture<Object> completableFuture = new CompletableFuture<>();
                    new Thread(() -> {
                        try {
                            completableFuture.complete(Optional.ofNullable(functionMethod.invoke(function, range(0, arguments.size()).boxed()
                                            .map(i -> jsonParser.parseString(arguments.get(i).toString(), functionMethod.getParameters()[i].getParameterizedType()))
                                            .toArray()))
                                    .orElse(""));
                        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException | JsonToObjectParsingException e) {
                            completableFuture.completeExceptionally(new WrongArgumentsException(functionMethod, e));
                        } catch (IllegalAccessException e) {
                            completableFuture.completeExceptionally(new ExecutionMethodNotAccessibleException(functionMethod, e));
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof PolyApiExecutionException expectedException) {
                                completableFuture.completeExceptionally(new PolyApiExecutionExceptionWrapperException(expectedException));
                            } else {
                                completableFuture.completeExceptionally(new UnexpectedFunctionExecutionException(e.getCause()));
                            }
                        } catch (RuntimeException e) {
                            completableFuture.completeExceptionally(e);
                        }
                    },
                            format("%sThread-%s", loggingEnabled ? LOGGING_THREAD_PREFIX : "", UUID.randomUUID()))
                            .start(); // Thread name defined by adding logs or not. This way the PolyAppender can know if it should log this or not.
                    Object methodResult = completableFuture.get();
                    log.info("Function executed successfully.");
                    log.info("Handling response.");
                    Message<?> result;
                    if (methodResult instanceof Number || methodResult instanceof String) {
                        log.debug("Result is a number or a string. Skipping conversion to JSon.");
                        result = MessageBuilder.withPayload(methodResult).build();
                    } else {
                        log.debug("Result is not a number nor a string. Converting to Json.");
                        result = MessageBuilder.withPayload(jsonParser.toJsonString(methodResult))
                                .setHeader("Content-Type", "application/json")
                                .build();
                    }
                    log.trace("Response body is:\n {}", result.getPayload());
                    log.debug("Response handled successfully.");
                    log.info("Function execution complete.");
                    return result;
                } catch (JsonToObjectParsingException e) {
                    throw new WrongArgumentsException(functionMethod, e);
                } catch (InterruptedException e) {
                    throw new UnexpectedFunctionExecutionException(e);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof PolyKNativeFunctionException cause) {
                        throw cause;
                    } else {
                        throw new UnexpectedFunctionExecutionException(e);
                    }
                }
            } catch (NoSuchMethodException e) {
                throw new ConstructorNotFoundException(e);
            } catch (InvocationTargetException e) {
                throw new FunctionCreationException(e);
            } catch (InstantiationException e) {
                throw new ClassNotInstantiableException(functionQualifiedName, e);
            } catch (IllegalAccessException e) {
                throw new ConstructorNotAccessibleException(e);
            }
        } catch (ClassNotFoundException e) {
            throw new PolyFunctionNotFoundException(e);
        }
    }
}
