package io.polyapi.knative.function;

import io.polyapi.commons.internal.json.JacksonJsonParser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Setter
@Slf4j
public class KNativeFunction {

    public static void main(String[] args) {
        SpringApplication.run(KNativeFunction.class, args);
    }

    @Bean
    public JacksonJsonParser objectMapper() {
        return new JacksonJsonParser();
    }
}
