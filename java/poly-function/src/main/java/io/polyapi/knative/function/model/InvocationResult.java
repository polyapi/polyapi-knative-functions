package io.polyapi.knative.function.model;

import io.polyapi.client.api.model.function.PolyCustom;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public class InvocationResult {

    private final Optional<Object> data;
    private final PolyCustom metadata;

    public InvocationResult(Object data, PolyCustom metadata) {
        this.data = Optional.ofNullable(data);
        this.metadata = metadata;
    }
}
