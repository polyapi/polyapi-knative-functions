package io.polyapi.knative.function.mock.function;

import java.util.function.Supplier;

public class StatefulObjectSupplier implements Supplier<StatefulObject> {

    @Override
    public StatefulObject get() {
        return new StatefulObject(true);
    }
}
