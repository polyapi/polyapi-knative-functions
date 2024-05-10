package io.polyapi.knative.function.mock;

import lombok.Getter;
import lombok.Setter;

import java.util.Random;

/**
 * This is a mock class. Do not delete nor change the package. Qualified name should be io.polyapi.knative.function.PolyCustomFunction.
 */
public class PolyCustomFunction {

    @Setter
    @Getter
    public static class Person {
        private String name;
        private Integer age;
    }

    public Person execute(String param) {
        Person person = new Person();
        person.setName(param);
        person.setAge((int)(Math.random() * 100));
        return person;
    }
}
