package io.polyapi.knative.function.mock.function;

import java.util.function.Consumer;

public class StatefulObjectConsumer implements Consumer<StatefulObject> {

    @Override
    public void accept(StatefulObject object) {
        object.setModified(true);
    }
}
