package io.polyapi.knative.function.invocation;

import io.polyapi.commons.api.json.JsonParser;
import io.polyapi.knative.function.model.FunctionArguments;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultInvocationStrategy extends InvocationStrategy {

    public DefaultInvocationStrategy(JsonParser jsonParser) {
        super(jsonParser);
    }

    @Override
    public FunctionArguments parsePayload(String payload) {
        log.debug("Lack of 'ce-id' header indicates the function is invoked normally.");
        return parseString(payload, FunctionArguments.class);
    }

    @Override
    public String getName() {
        return "Default";
    }
}
