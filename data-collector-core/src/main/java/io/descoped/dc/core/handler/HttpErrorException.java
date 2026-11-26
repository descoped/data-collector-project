package io.descoped.dc.core.handler;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when a response has a status code that the request's validation does not accept.
 */
public class HttpErrorException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception without a message.
     */
    public HttpErrorException() {
    }

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public HttpErrorException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public HttpErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param cause the cause
     */
    public HttpErrorException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with a message, a cause, and suppression and a writable stack trace on or off.
     *
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace is writable
     */
    public HttpErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
