package io.descoped.dc.api.error;

/**
 * Thrown when the execution of a specification fails.
 */
public class ExecutionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public ExecutionException() {
    }

    /**
     * Creates the exception with a message.
     *
     * @param message the message
     */
    public ExecutionException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param cause the cause
     */
    public ExecutionException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with the options of {@link RuntimeException}.
     *
     * @param message            the message
     * @param cause              the cause
     * @param enableSuppression  whether suppression is enabled
     * @param writableStackTrace whether the stack trace is writable
     */
    public ExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
