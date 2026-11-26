package io.descoped.dc.core.handler;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when an HTTP request times out.
 */
public class HttpRequestTimeoutException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message the message
     */
    public HttpRequestTimeoutException(String message) {
        super(message);
    }
}
