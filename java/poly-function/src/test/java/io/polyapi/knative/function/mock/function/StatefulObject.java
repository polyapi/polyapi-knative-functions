package io.polyapi.knative.function.mock.function;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class StatefulObject {
    private boolean modified;
}
