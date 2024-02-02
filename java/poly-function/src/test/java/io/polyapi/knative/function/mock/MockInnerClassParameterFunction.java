package io.polyapi.knative.function.mock;

import lombok.Getter;
import lombok.Setter;

public class MockInnerClassParameterFunction {

    @Getter
    @Setter
    public static class InnerClass {
        private String name;
    }

    public String execute(InnerClass innerClass) {
        return innerClass.name;
    }
}
