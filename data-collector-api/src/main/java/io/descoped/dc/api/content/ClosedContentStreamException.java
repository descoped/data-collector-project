package io.descoped.dc.api.content;

/**
 * Thrown when a closed content stream, producer or consumer is used.
 */
public class ClosedContentStreamException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public ClosedContentStreamException() {
        super();
    }

    /**
     * Creates the exception with a message.
     *
     * @param message the message
     */
    public ClosedContentStreamException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and a cause.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ClosedContentStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param cause the cause
     */
    public ClosedContentStreamException(Throwable cause) {
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
    protected ClosedContentStreamException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
