package io.polyapi.knative.function.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TriggerEventResult {
    private final Object data;
    private final Integer statusCode;
    private final String executionId;
    private final String functionId;
    private final String environmentId;
    private final String contentType;
    private final Metrics metrics;
}