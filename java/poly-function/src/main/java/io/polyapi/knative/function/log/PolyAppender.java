package io.polyapi.knative.function.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

public class PolyAppender extends ConsoleAppender<ILoggingEvent> {
    public static final String LOGGING_THREAD_PREFIX = "Poly-log-";

    protected boolean isLoggingThread(ILoggingEvent eventObject) {
        return eventObject.getThreadName().startsWith(LOGGING_THREAD_PREFIX);
    }
}
