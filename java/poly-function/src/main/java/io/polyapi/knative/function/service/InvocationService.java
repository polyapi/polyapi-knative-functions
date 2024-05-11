package io.polyapi.knative.function.service;

import java.lang.reflect.Method;

/**
 * Interface of the function server invocation.
 */
public interface InvocationService {

    /**
     * Invokes the server function.
     *
     * @param clazz The class that contains the function to be invoked.
     * @param method The method of the function to execute.
     * @param arguments  The arguments with which the function will be executed.
     * @param logsEnabled Flag indicating if logs should be enabled.
     * @return Object The result of the function call. Void if no result is available.
     */
    Object invokeFunction(Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled);
}
