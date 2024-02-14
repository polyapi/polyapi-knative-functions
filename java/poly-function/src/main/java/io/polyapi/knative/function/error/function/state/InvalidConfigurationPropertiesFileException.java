package io.polyapi.knative.function.error.function.state;

import static java.lang.String.format;

/**
 * Exception thrown when the poly.properties file cannot be found.
 */
public class InvalidConfigurationPropertiesFileException extends PolyFunctionStateException {
    public InvalidConfigurationPropertiesFileException(String propertiesFile, Throwable cause) {
        super(format("File '%s' containing the configuration of the server function is invalid.", propertiesFile), cause);
    }
}
