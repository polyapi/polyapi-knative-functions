package io.polyapi.knative.function;

import io.polyapi.commons.api.error.PolyApiExecutionException;
import io.polyapi.commons.api.error.parse.JsonToObjectParsingException;
import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.function.creation.FunctionCreationException;
import io.polyapi.knative.function.error.function.execution.PolyApiExecutionExceptionWrapperException;
import io.polyapi.knative.function.error.function.execution.UnexpectedFunctionExecutionException;
import io.polyapi.knative.function.error.function.execution.WrongArgumentsException;
import io.polyapi.knative.function.error.function.state.*;
import io.polyapi.knative.function.log.PolyLogStream;
import io.polyapi.knative.function.model.FunctionArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import static java.lang.Boolean.FALSE;
import static java.util.function.Predicate.not;
import static java.util.stream.IntStream.range;

@SpringBootApplication
public class KNativeFunction {
    private final static Logger logger = LoggerFactory.getLogger(KNativeFunction.class);
    private final String propertiesFile;

    private final JsonParser jsonParser = new JacksonJsonParser();

    public static void main(String[] args) {
        SpringApplication.run(KNativeFunction.class, args);
    }

    public KNativeFunction() {
        this("/poly.properties");
    }

    public KNativeFunction(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    @Bean
    public Function<Message<String>, Message<?>> execute() {
        return this::process;
    }

    private Message<?> process(Message<String> inputMessage) {
        try {
            boolean loggingEnabled = Optional.ofNullable(inputMessage.getHeaders().get("x-poly-do-log"))
                    .map(Object::toString)
                    .map(Boolean::getBoolean)
                    .orElse(FALSE);
            System.setOut(new PolyLogStream(System.out, "INFO", loggingEnabled));
            System.setErr(new PolyLogStream(System.err, "ERROR", loggingEnabled));
            Properties properties = new Properties();
            logger.info("Obtaining configuration from {} file.", propertiesFile);
            properties.load(Optional.ofNullable(propertiesFile).map(KNativeFunction.class::getResourceAsStream).orElseThrow(() -> new ConfigurationPropertiesFileNotFoundException(propertiesFile)));
            logger.debug("Configuration retrieved successfully.");
            String functionQualifiedName = properties.getProperty("polyapi.function.class", "");
            logger.info("Loading class {}.", functionQualifiedName);
            Class<?> functionClass = Class.forName(functionQualifiedName);
            logger.debug("Class {} loaded successfully.", functionQualifiedName);
            String parameterTypes = properties.getProperty("polyapi.function.params", "");
            logger.info("Loading parameter types: [{}].", parameterTypes);
            Class<?>[] paramTypes = Optional.ofNullable(parameterTypes)
                    .filter(not(String::isBlank))
                    .map(params -> params.split(","))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(String::trim)
                    .map(qualifiedName -> {
                        try {
                            logger.debug("Loading class for parameter type '{}'.", qualifiedName);
                            Class<?> result = Class.forName(qualifiedName);
                            logger.debug("Class loaded successfully.");
                            return result;
                        } catch (ClassNotFoundException e) {
                            throw new InvalidArgumentTypeException(qualifiedName, e);
                        }
                    })
                    .toArray(Class<?>[]::new);
            logger.debug("Parameter types loaded successfully.");
            String methodName = properties.getProperty("polyapi.function.method", "");
            try {
                logger.info("Retrieving method {}.{}({}).", functionQualifiedName, methodName, parameterTypes);
                Method functionMethod = functionClass.getDeclaredMethod(methodName, paramTypes);
                logger.debug("Method {} retrieved successfully.", functionMethod);
                logger.debug("Executing function with payload {}.", inputMessage.getPayload());
                try {
                    logger.info("Retrieving default constructor to setup the server function.");
                    Constructor<?> constructor = functionClass.getDeclaredConstructor();
                    logger.debug("Default constructor retrieved successfully.");
                    logger.info("Instantiating function class {} using default constructor.", functionQualifiedName);
                    Object function = constructor.newInstance();
                    logger.info("Class {} instantiated successfully.", functionQualifiedName);
                    try {
                        logger.debug("Parsing payload.");
                        FunctionArguments arguments = jsonParser.parseString(inputMessage.getPayload(), FunctionArguments.class);
                        logger.debug("Parse successful.");
                        logger.info("Executing function.");
                        Object methodResult = Optional.ofNullable(functionMethod.invoke(function, range(0, arguments.size()).boxed()
                                        .map(i -> jsonParser.parseString(arguments.get(i).toString(), paramTypes[i]))
                                        .toArray()))
                                .orElse("");
                        logger.info("Function executed successfully.");
                        logger.info("Handling response.");
                        Message<?> result;
                        if (methodResult instanceof Number || methodResult instanceof String) {
                            logger.debug("Result is a number or a string. Skipping conversion to JSon.");
                            result = MessageBuilder.withPayload(methodResult).build();
                        } else {
                            logger.debug("Result is not a number nor a string. Converting to Json.");
                            result = MessageBuilder.withPayload(jsonParser.toJsonString(methodResult))
                                    .setHeader("Content-Type", "application/json")
                                    .build();
                        }
                        if (logger.isTraceEnabled()) {
                            logger.trace("Response body is:\n {}", result.getPayload());
                        }
                        logger.debug("Response handled successfully.");
                        logger.info("Function execution complete.");
                        return result;
                    } catch (JsonToObjectParsingException | ArrayIndexOutOfBoundsException |
                             IllegalArgumentException e) {
                        throw new WrongArgumentsException(functionMethod, e);
                    } catch (IllegalAccessException e) {
                        throw new ExecutionMethodNotAccessibleException(functionMethod, e);
                    } catch (InvocationTargetException e) {
                        if (e.getTargetException() instanceof PolyApiExecutionException expectedException) {
                            throw new PolyApiExecutionExceptionWrapperException(expectedException);
                        } else {
                            throw new UnexpectedFunctionExecutionException(e.getTargetException());
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
            } catch (NoSuchMethodException e) {
                throw new ExecutionMethodNotFoundException(methodName, parameterTypes, e);
            }
        } catch (ClassNotFoundException e) {
            throw new PolyFunctionNotFoundException(e);
        } catch (IllegalArgumentException | IOException e) {
            throw new InvalidConfigurationPropertiesFileException(this.propertiesFile, e);
        }
    }
}
