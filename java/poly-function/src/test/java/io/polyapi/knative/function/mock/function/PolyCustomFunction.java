package io.polyapi.knative.function.mock.function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class PolyCustomFunction {
    public static final String DEFAULT_RESULT = "Result";
    @Getter
    @Setter
    @AllArgsConstructor
    public static class DonRamon {
        private String name;
        private Integer age;
    }

    public DonRamon execute(String param) {
        return new DonRamon(param, (int)(Math.random() * 100));
    }
}
