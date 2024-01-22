package io.polyapi.knative.function.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Setter
public class FunctionArguments {
    private List<Object> args;

    public List<Object> list() {
        return Optional.ofNullable(args).orElseGet(ArrayList::new);
    }

    public Object get(int position) {
        return list().get(position);
    }

    public Stream<Class<?>> stream() {
        return list().stream().map(Object::getClass);
    }

    public int size() {
        return list().size();
    }
}
