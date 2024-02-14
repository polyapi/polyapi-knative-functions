package io.polyapi.knative.function.error.function.state;

import static java.lang.String.format;

/**
 * Exception thrown when the poly.properties file cannot be found.
 */
public class ConfigurationPropertiesFileNotFoundException extends PolyFunctionStateException {
    public ConfigurationPropertiesFileNotFoundException(String propertiesFile) {
        super(format("File '%s' containing the configuration of the server function not found.", propertiesFile));
    }
}
