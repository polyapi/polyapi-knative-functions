package io.polyapi.knative.function.mock.exception;

import io.polyapi.knative.function.error.PolyKNativeFunctionException;

public class MockServiceException extends PolyKNativeFunctionException {
    public static final String MESSAGE = "Error!";
    public static final Integer STATUS_CODE = 400;
    public MockServiceException() {
        super(MESSAGE, STATUS_CODE);
    }
}
