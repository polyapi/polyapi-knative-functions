package io.polyapi.knative.function.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Metrics {
    private final Long start;
    private final Long end;
}
