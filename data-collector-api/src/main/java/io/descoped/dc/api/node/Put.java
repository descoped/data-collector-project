package io.descoped.dc.api.node;

/**
 * An HTTP PUT operation.
 */
public interface Put extends Operation {

    /**
     * The request body.
     *
     * @return the body publisher
     */
    BodyPublisher bodyPublisher();

}
