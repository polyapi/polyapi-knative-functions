package io.polyapi.knative.function.mock.function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntFunction {

    public String apply(int argument) {
        log.info("Argument: {}", argument);
        return String.valueOf(argument);
    }
}
