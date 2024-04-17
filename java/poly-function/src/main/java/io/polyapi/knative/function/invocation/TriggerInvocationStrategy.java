package io.polyapi.knative.function.invocation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.knative.function.error.function.execution.MissingHeaderException;
import io.polyapi.knative.function.model.FunctionArguments;
import io.polyapi.knative.function.model.Metrics;
import io.polyapi.knative.function.model.TriggerEventResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class TriggerInvocationStrategy extends InvocationStrategy {

    private static final String EXECUTION_ID_HEADER = "x-poly-execution-id";
    private static final String ENVIRONMENT_ID_HEADER = "ce-environment";
    private final long start;
    private final String functionId;

    public TriggerInvocationStrategy(JsonParser jsonParser, String functionId) {
        super(jsonParser);
        this.functionId = functionId;
        this.start = System.currentTimeMillis();
    }

    @Override
    public FunctionArguments parsePayload(String payload) {
        log.debug("Presence of 'ce-id' header indicates that the function is invoked from a trigger.");
        return new FunctionArguments(parseString(payload, TypeFactory.defaultInstance().constructCollectionType(List.class, JsonNode.class)));
    }

    @Override
    public Message<?> parseResult(Object methodResult, Map<String, Object> headers) {
        return MessageBuilder.withPayload(toJsonString(new TriggerEventResult(methodResult,
                        200,
                        Optional.ofNullable(headers.get(EXECUTION_ID_HEADER)).map(Object::toString).orElseThrow(() -> new MissingHeaderException(EXECUTION_ID_HEADER)),
                        functionId,
                        Optional.ofNullable(headers.get(ENVIRONMENT_ID_HEADER)).map(Object::toString).orElseThrow(() -> new MissingHeaderException(ENVIRONMENT_ID_HEADER)),
                        APPLICATION_JSON_VALUE,
                        new Metrics(start, System.currentTimeMillis()))))
                .copyHeaders(headers).build();
    }
}
