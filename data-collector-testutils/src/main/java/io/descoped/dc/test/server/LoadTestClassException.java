package io.descoped.dc.test.server;

/**
 * Thrown when a test class cannot be loaded.
 */
public class LoadTestClassException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param cause the cause
     */
    public LoadTestClassException(Throwable cause) {
        super(cause);
    }

}
