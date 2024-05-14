package io.polyapi.knative.function.service;

import io.polyapi.knative.function.model.InvocationResult;

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
     * @param executionId The execution ID of the function.
     * @return InvocationResult The result of the function call. Contains the data with the function result (null in case no result is returned) and the PolyCustom metadata.
     */
    InvocationResult invokeFunction(Class<?> clazz, Method method, Object[] arguments, boolean logsEnabled, String executionId);
}
