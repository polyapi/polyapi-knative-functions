package io.polyapi.knative.function.mock;

import lombok.Getter;
import lombok.Setter;

public class InnerClassParameterFunction {

    @Getter
    @Setter
    public static class InnerClass {
        private String name;
    }

    public String execute(InnerClass innerClass) {
        return innerClass.name;
    }
}
