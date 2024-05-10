package io.polyapi.knative.function.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.polyapi.knative.function.error.PolyFunctionError;
import io.polyapi.knative.function.error.PolyKNativeFunctionException;
import io.polyapi.knative.function.model.Metrics;
import io.polyapi.knative.function.model.TriggerEventResult;
import io.polyapi.knative.function.service.invocation.InvocationService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Slf4j
@RestController
@Setter
public class InvocationController {
    private static final String TYPE_HEADER = "ce-type";

    @Value("${polyapi.function.id:}")
    private String functionId;

    @Autowired
    private InvocationService invocationService;

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = {APPLICATION_JSON_VALUE, TEXT_PLAIN_VALUE})
    public ResponseEntity<?> invoke(@RequestHeader(name = "x-poly-do-log", required = false, defaultValue = "false") boolean logEnabled,
                                    @RequestBody Map<String, List<JsonNode>> arguments) {
        Object methodResult = invocationService.invokeFunction(arguments.get("args").stream().map(Object::toString).toList(), logEnabled);
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
    public ResponseEntity<?> handleMessage(@RequestHeader HttpHeaders headers,
                                           @RequestHeader(name = "x-poly-do-log", required = false, defaultValue = "false") boolean logEnabled,
                                           @RequestHeader("ce-executionid") String executionId,
                                           @RequestHeader("ce-environment") String environmentId,
                                           @RequestBody List<JsonNode> arguments) {
        Long start = System.currentTimeMillis();
        log.debug("Presence of 'ce-id' header indicates that the function is invoked from a trigger.");
        Object invocationResult = invocationService.invokeFunction(arguments.stream().map(Object::toString).toList(), logEnabled);
        log.info("Function executed successfully.");
        log.info("Handling response.");
        ResponseEntity<?> result = ResponseEntity.ok().headers(headers)
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
}
