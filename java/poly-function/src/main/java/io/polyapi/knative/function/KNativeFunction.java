package io.polyapi.knative.function;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.commons.internal.json.JacksonJsonParser;
import io.polyapi.knative.function.error.function.*;
import io.polyapi.knative.function.log.PolyLogStream;
import io.polyapi.knative.function.model.FunctionArguments;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Boolean.FALSE;
import static java.util.stream.IntStream.range;

@SpringBootApplication
@Setter
public class KNativeFunction {
    private final static Logger logger = LoggerFactory.getLogger(KNativeFunction.class);
    private final Supplier<Object> functionSupplier;

    private JsonParser jsonParser = new JacksonJsonParser();

    public static void main(String[] args) {
        SpringApplication.run(KNativeFunction.class, args);
    }

    public KNativeFunction() {
        this(() -> {
            try {
                return Class.forName("io.polyapi.knative.function.PolyCustomFunction").getConstructor().newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                throw new ConstructorNotFoundException(e);
            } catch (InvocationTargetException | InstantiationException e) {
                throw new FunctionCreationException(e);
            }
        });
    }

    public KNativeFunction(Supplier<Object> functionSupplier) {
        this.functionSupplier = functionSupplier;
    }

    @Bean
    public Function<Message<String>, Message<String>> execute() {
        return Function.<Message<String>>identity()
                .andThen(this::process)
                .andThen(result -> MessageBuilder.withPayload(Optional.ofNullable(result)
                                .map(jsonParser::toJsonString)
                                .orElse(""))
                        .copyHeaders(result instanceof Number ? Map.of() : Map.of("Content-Type", "application/json"))
                        .build());
    }

    private Object process(Message<String> inputMessage) {
        boolean loggingEnabled = Optional.ofNullable(inputMessage.getHeaders().get("x-poly-do-log"))
                .map(Object::toString)
                .map(Boolean::getBoolean)
                .orElse(FALSE);
        System.setOut(new PolyLogStream(System.out, "INFO", loggingEnabled));
        System.setErr(new PolyLogStream(System.err, "ERROR", loggingEnabled));
        logger.debug("Executing function with payload {}.", inputMessage.getPayload());
        FunctionArguments arguments = jsonParser.parseString(inputMessage.getPayload(), FunctionArguments.class);
        Object function = functionSupplier.get();
        Object result = Arrays.stream(function.getClass().getDeclaredMethods())
                .filter(method -> method.getName().equals("execute"))
                .findFirst()
                .map(executeMethod -> {
                    try {
                        logger.debug("Executing method '{}'", executeMethod);
                        Parameter[] parameters = executeMethod.getParameters();
                        return Optional.ofNullable(executeMethod.invoke(function,
                                        range(0, arguments.size()).boxed()
                                                .map(i -> Optional.ofNullable(arguments.get(i))
                                                        .filter(Number.class::isInstance)
                                                        .map(Object::toString)
                                                        .map(switch (parameters[i].getType().getName()) {
                                                            case "java.lang.Integer" -> Integer::valueOf;
                                                            case "java.lang.Long" -> Long::valueOf;
                                                            case "java.lang.Double" -> Double::valueOf;
                                                            case "java.lang.Float" -> Float::valueOf;
                                                            case "java.lang.Short" -> Short::valueOf;
                                                            case "java.lang.Byte" -> Byte::valueOf;
                                                            default -> Function.<Object>identity();
                                                        })
                                                        .orElse(arguments.get(i)))
                                                .toArray()));
                    } catch (InvocationTargetException e) {
                        throw new FunctionInvocationException(e);
                    } catch (IllegalAccessException e) {
                        throw new ExecuteMethodNotFoundException(executeMethod, e);
                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                        throw new WrongNumberOfArgumentsException(executeMethod, arguments, e);
                    }
                })
                .orElseThrow(ExecuteMethodNotFoundException::new)
                .orElse(null);
        logger.debug("Execution successful.");
        if (logger.isTraceEnabled()) {
            logger.trace("Response body is:\n {}", jsonParser.toJsonString(result));
        }
        return result;
    }
}
