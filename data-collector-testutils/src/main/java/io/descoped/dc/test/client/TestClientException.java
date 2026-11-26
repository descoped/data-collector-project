package io.descoped.dc.test.client;

/**
 * Thrown when the test client cannot send a request or read its response.
 */
public class TestClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public TestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception.
     *
     * @param cause the cause
     */
    public TestClientException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception.
     *
     * @param message the message
     */
    public TestClientException(String message) {
        super(message);
    }
}
