package io.descoped.dc.api.node;

import java.util.List;

/**
 * The body of a POST or PUT request: plain text, URL encoded data or multipart parts.
 */
public interface BodyPublisher extends OperationPublisher {

    /**
     * The key of the execution state that holds the body publisher the node produced.
     */
    String BODY_PUBLISHER_RESULT = "BODY_PUBLISHER_RESULT";

    /**
     * The body's encoding.
     *
     * @return the encoding, or {@code null} if no body is set
     */
    FormEncoding getEncoding();

    /**
     * The producer of a plain text body.
     *
     * @return the producer, or {@code null} if none is set
     */
    BodyPublisherProducer getPlainText();

    /**
     * The producer of a URL encoded body.
     *
     * @return the producer, or {@code null} if none is set
     */
    BodyPublisherProducer getUrlEncodedData();

    /**
     * The parts of a multipart body.
     *
     * @return the parts
     */
    List<BodyPart> getParts();

}
