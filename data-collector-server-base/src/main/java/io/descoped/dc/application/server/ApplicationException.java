package io.descoped.dc.application.server;

/**
 * Thrown when the application cannot be configured, started or discovered.
 */
public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the message
     */
    public ApplicationException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception.
     *
     * @param cause the cause
     */
    public ApplicationException(Throwable cause) {
        super(cause);
    }
}
