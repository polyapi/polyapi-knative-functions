package io.polyapi.knative.function.error.function.state;

/**
 * Exception thrown when the poly.properties file cannot be found.
 */
public class ConfigurationPropertiesNotFoundException extends PolyFunctionStateException {
    public ConfigurationPropertiesNotFoundException(Throwable cause) {
        super("File 'poly.properties' containing the configuration of the server function not found.", cause);
    }
}
