package io.polyapi.knative.function.service.invocation;

import java.util.List;

/**
 * Interface of the function server invocation.
 */
public interface InvocationService {

    /**
     * Invokes the server function.
     *
     * @param arguments  The arguments with which the function will be executed.
     * @param logEnabled Flag indicating if logs should be enabled.
     * @return Object The result of the function call. Void if no result is available.
     */
    Object invokeFunction(List<String> arguments, boolean logEnabled);
}
