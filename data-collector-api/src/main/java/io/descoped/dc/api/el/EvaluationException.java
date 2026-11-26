package io.descoped.dc.api.el;

import io.descoped.dc.api.error.ExecutionException;

/**
 * Thrown when an expression cannot be evaluated.
 */
public class EvaluationException extends ExecutionException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public EvaluationException() {
        super();
    }

    /**
     * Creates the exception with a message.
     *
     * @param message the message
     */
    public EvaluationException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param cause the cause
     */
    public EvaluationException(Throwable cause) {
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
    public EvaluationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
