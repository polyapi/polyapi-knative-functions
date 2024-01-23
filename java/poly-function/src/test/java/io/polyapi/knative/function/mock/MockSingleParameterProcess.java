package io.polyapi.knative.function.mock;

import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MockSingleParameterProcess {

    private final Consumer<Integer> assertionsBlock;

    public MockSingleParameterProcess() {
        this(param -> assertThat(param, notNullValue()));
    }

    public MockSingleParameterProcess(Consumer<Integer> assertionsBlock) {
        this.assertionsBlock = assertionsBlock;
    }

    public void execute(Integer param) {
        assertionsBlock.accept(param);
    }
}
