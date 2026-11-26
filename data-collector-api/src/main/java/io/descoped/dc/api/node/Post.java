package io.descoped.dc.api.node;

/**
 * An HTTP POST operation.
 */
public interface Post extends Operation {

    /**
     * The request body.
     *
     * @return the body publisher
     */
    BodyPublisher bodyPublisher();

}
