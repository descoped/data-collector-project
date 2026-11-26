package io.descoped.dc.test.server;

/**
 * Thrown when the test server cannot be created or reached.
 */
public class TestServerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the message
     */
    public TestServerException(String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param cause the cause
     */
    public TestServerException(Throwable cause) {
        super(cause);
    }
}
