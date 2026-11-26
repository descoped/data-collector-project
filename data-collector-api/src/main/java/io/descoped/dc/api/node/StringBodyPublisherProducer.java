package io.descoped.dc.api.node;

/**
 * A producer of a request body from a text.
 */
public interface StringBodyPublisherProducer extends BodyPublisherProducer {

    /**
     * The text; it may hold expressions.
     *
     * @return the text
     */
    String text();

}
