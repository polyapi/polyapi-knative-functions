package io.polyapi.knative.function.mock;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MockSingleParameterProcess {

    private final Consumer<String> assertionsBlock;

    public MockSingleParameterProcess() {
        this(param -> assertThat(param, notNullValue()));
    }

    public MockSingleParameterProcess(Consumer<String> assertionsBlock) {
        this.assertionsBlock = assertionsBlock;
    }

    public void execute(String param) {
        assertionsBlock.accept(param);
    }
}
