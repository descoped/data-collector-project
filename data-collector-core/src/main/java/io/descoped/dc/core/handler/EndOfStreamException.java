package io.descoped.dc.core.handler;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when a stream has no more data.
 */
public class EndOfStreamException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception without a message.
     */
    public EndOfStreamException() {
        super();
    }

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public EndOfStreamException(String message) {
        super(message);
    }

    /**
     * Creates an exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EndOfStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with a cause.
     *
     * @param cause the cause
     */
    public EndOfStreamException(Throwable cause) {
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
    protected EndOfStreamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
