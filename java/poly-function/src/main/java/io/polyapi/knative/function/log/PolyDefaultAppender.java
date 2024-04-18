package io.polyapi.knative.function.log;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class PolyDefaultAppender extends PolyAppender {
    @Override
    protected void append(ILoggingEvent eventObject) {
        if (isLoggingThread(eventObject)) {
            super.append(eventObject);
        }
    }
}
