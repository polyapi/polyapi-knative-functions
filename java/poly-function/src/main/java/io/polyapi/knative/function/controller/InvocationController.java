package io.polyapi.knative.function.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.polyapi.commons.api.error.parse.JsonToObjectParsingException;
import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.knative.function.controller.dto.Metrics;
import io.polyapi.knative.function.controller.dto.TriggerEventResult;
import io.polyapi.knative.function.error.PolyFunctionError;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.error.function.state.ExecutionMethodNotFoundException;
import io.polyapi.knative.function.error.function.state.InvalidArgumentTypeException;
import io.polyapi.knative.function.error.function.state.PolyFunctionNotFoundException;
import io.polyapi.knative.function.service.InvocationService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.function.Predicate.not;
import static java.util.stream.IntStream.range;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Slf4j
@Setter
@RestController
public class InvocationController {
    private static final String TYPE_HEADER = "ce-type";

    @Value("${polyapi.function.id:}")
    private String functionId;

    @Value("${polyapi.function.class:io.polyapi.knative.function.PolyCustomFunction}")
    private String functionQualifiedName;

    @Value("${polyapi.function.method:execute}")
    private String methodName;

    @Value("${polyapi.function.params:#{null}}")
    private String parameterTypes;

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private InvocationService invocationService;

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = {APPLICATION_JSON_VALUE, TEXT_PLAIN_VALUE})
    public ResponseEntity<?> invoke(@RequestHeader(name = "x-poly-do-log", required = false, defaultValue = "false") boolean logsEnabled,
                                    @RequestBody Map<String, List<JsonNode>> arguments) {
        log.info("Poly logs are {}enabled for this function execution.", logsEnabled ? "" : "not ");
        Object methodResult = invokeFunction(arguments.get("args"), logsEnabled);
        ResponseEntity<?> result;
        if (methodResult instanceof Number || methodResult instanceof String) {
            log.debug("Result is a number or a string. Skipping conversion to JSon.");
            result = ResponseEntity.ok(methodResult);
        } else {
            log.debug("Result is not a number nor a string. Converting to Json.");
            result = ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(methodResult);
        }
        return result;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE, headers = "ce-id")
    public ResponseEntity<TriggerEventResult> trigger(@RequestHeader HttpHeaders headers,
                                                      @RequestHeader(name = "x-poly-do-log", required = false, defaultValue = "false") boolean logsEnabled,
                                                      @RequestHeader("ce-executionid") String executionId,
                                                      @RequestHeader("ce-environment") String environmentId,
                                                      @RequestBody List<JsonNode> arguments) {
        log.info("Poly logs are {}enabled for this function execution.", logsEnabled ? "" : "not ");
        Long start = System.currentTimeMillis();
        log.debug("Presence of 'ce-id' header indicates that the function is invoked from a trigger.");
        Object invocationResult = invokeFunction(arguments, logsEnabled);
        log.info("Function executed successfully.");
        log.info("Handling response.");
        ResponseEntity<TriggerEventResult> result = ResponseEntity.ok().headers(headers)
                .headers(outputHeaders -> {
                    outputHeaders.remove(CONTENT_LENGTH);
                    outputHeaders.remove(CONTENT_LENGTH.toLowerCase());
                    outputHeaders.remove(TYPE_HEADER);
                })
                .header(TYPE_HEADER, "trigger.response")
                .body(new TriggerEventResult(200,
                        executionId,
                        functionId,
                        environmentId,
                        APPLICATION_JSON_VALUE,
                        new Metrics(start, System.currentTimeMillis()),
                        invocationResult));
        log.trace("Response headers are:\n");
        result.getHeaders().forEach((key, value) -> log.trace("    \"{}\": \"{}\"", key, value));
        log.debug("Response handled successfully.");
        return result;
    }

    @ExceptionHandler(PolyKNativeFunctionException.class)
    public ResponseEntity<PolyFunctionError> handleException(PolyKNativeFunctionException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getStatusCode()).body(exception.toErrorObject());
    }

    @ExceptionHandler({JsonToObjectParsingException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<PolyFunctionError> handleParsingException(RuntimeException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.badRequest().body(new PolyFunctionError(BAD_REQUEST.value(), exception.getMessage()));
    }

    private Object invokeFunction(List<JsonNode> arguments, boolean logsEnabled) {
        try {
            log.info("Loading class {}.", functionQualifiedName);
            Class<?> functionClass = Class.forName(functionQualifiedName);
            log.debug("Class {} loaded successfully.", functionQualifiedName);
            Method functionMethod;
            Class<?>[] paramTypes;
            if (parameterTypes == null) {
                functionMethod = Arrays.stream(functionClass.getDeclaredMethods()).filter(method -> method.getName().equals(methodName)).findFirst().orElseThrow(() -> new ExecutionMethodNotFoundException(methodName));
            } else {
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
                log.debug("Method {} retrieved successfully.", functionMethod);
            }
            return Optional.ofNullable(invocationService.invokeFunction(functionClass, functionMethod, range(0, arguments.size()).boxed()
                            .map(i -> jsonParser.parseString(arguments.get(i).toString(), functionMethod.getParameters()[i].getParameterizedType()))
                            .toArray(), logsEnabled))
                    .orElse("");
        } catch (NoSuchMethodException e) {
            throw new ExecutionMethodNotFoundException(methodName, parameterTypes, e);
        } catch (ClassNotFoundException e) {
            throw new PolyFunctionNotFoundException(e);
        }
    }
}
