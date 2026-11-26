package io.descoped.dc.api.handler;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when a query cannot be evaluated.
 */
public class QueryException extends ExecutionException {

    private static final long serialVersionUID = 1L;
    /**
     * Creates the exception.
     */
    public QueryException() {
        super();
    }

    /**
     * Creates the exception with a message.
     *
     * @param message the message
     */
    public QueryException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param cause the cause
     */
    public QueryException(Throwable cause) {
        super(cause);
    }
}
