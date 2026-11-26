package io.descoped.dc.api.handler;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * Parses and serializes the documents of a query type (JSON or XML).
 */
public interface DocumentParserFeature {

    /**
     * Serializes a document.
     *
     * @param document the document
     * @return the bytes
     */
    byte[] serialize(Object document);

    /**
     * Parses a document.
     *
     * @param source the bytes
     * @return the document
     */
    Object deserialize(byte[] source);

    /**
     * Reads the entries of a document from a stream, one at a time.
     *
     * @param source        the stream
     * @param entryCallback called with each entry
     */
    void tokenDeserializer(InputStream source, Consumer<Object> entryCallback);

}
