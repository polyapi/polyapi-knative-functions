package io.polyapi.knative.function.invocation;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.knative.function.error.function.execution.MissingPayloadException;
import io.polyapi.knative.function.model.FunctionArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

@Slf4j
public abstract class InvocationStrategy {

    private final JsonParser jsonParser;

    public InvocationStrategy(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    public FunctionArguments parsePayload(Object payload) {
        String stringPayload = Optional.of(payload).map(Object::toString).orElseThrow(MissingPayloadException::new);
        log.debug("Parsing payload.");
        log.trace("Payload: '{}'.", payload);
        FunctionArguments arguments = parsePayload(stringPayload);
        log.debug("Parse successful.");
        return arguments;
    }

    protected abstract FunctionArguments parsePayload(String payload);

    public Message<?> parseResult(Object methodResult, Map<String, Object> headers) {
        Message<?> result;
        if (methodResult instanceof Number || methodResult instanceof String) {
            log.debug("Result is a number or a string. Skipping conversion to JSon.");
            result = MessageBuilder.withPayload(methodResult).build();
        } else {
            log.debug("Result is not a number nor a string. Converting to Json.");
            result = MessageBuilder.withPayload(toJsonString(methodResult))
                    .setHeader("Content-Type", "application/json")
                    .build();
        }
        return result;
    }

    protected <T> T parseString(String json, Type type) {
        return jsonParser.parseString(json, type);
    }

    protected String toJsonString(Object object) {
        return jsonParser.toJsonString(object);
    }

    public abstract String getName();
}
