package io.descoped.dc.api;

/**
 * Thrown when an execution stops because it was terminated.
 */
public class TerminationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     */
    public TerminationException() {
        super();
    }

}
