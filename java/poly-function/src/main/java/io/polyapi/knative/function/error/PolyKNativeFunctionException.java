package io.polyapi.knative.function.error;

import io.polyapi.commons.api.error.PolyApiException;
import lombok.Getter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Wrapper class for all exceptions thrown on the execution of the Poly function.
 */
@Getter
public class PolyKNativeFunctionException extends PolyApiException {

    private final Integer statusCode;

    public PolyKNativeFunctionException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PolyKNativeFunctionException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Message<Throwable> toMessage() {
        return new ErrorMessage(new MessageHandlingException(null, this));
    }
}
