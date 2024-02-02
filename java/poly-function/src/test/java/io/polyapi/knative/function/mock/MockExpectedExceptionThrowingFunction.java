package io.polyapi.knative.function.mock;

import io.polyapi.commons.api.error.http.HttpResponseException;
import io.polyapi.commons.api.http.ResponseRecord;

import java.io.InputStream;
import java.util.Map;

import static java.io.InputStream.nullInputStream;

public class MockExpectedExceptionThrowingFunction {

    public void execute() {
        throw new HttpResponseException("Sample message", new ResponseRecord(Map.of(), nullInputStream(), 42));
    }
}
