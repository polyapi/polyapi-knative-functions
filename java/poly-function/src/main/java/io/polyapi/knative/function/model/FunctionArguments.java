package io.polyapi.knative.function.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
public class FunctionArguments {
    private List<JsonNode> args;

    public FunctionArguments() {
        this(new ArrayList<>());
    }

    public FunctionArguments(List<JsonNode> args) {
        this.args = args;
    }

    public List<JsonNode> list() {
        return Optional.ofNullable(args).orElseGet(ArrayList::new);
    }

    public JsonNode get(int position) {
        return list().get(position);
    }

    public int size() {
        return list().size();
    }
}
