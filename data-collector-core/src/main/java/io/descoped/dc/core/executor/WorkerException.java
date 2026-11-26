package io.descoped.dc.core.executor;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when a worker fails, or wraps a checked exception it met.
 */
public class WorkerException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the message
     */
    public WorkerException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public WorkerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception.
     *
     * @param cause the cause
     */
    public WorkerException(Throwable cause) {
        super(cause);
    }
}
