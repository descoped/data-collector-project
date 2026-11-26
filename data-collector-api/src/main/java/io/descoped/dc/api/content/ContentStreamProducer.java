package io.descoped.dc.api.content;

import java.util.Arrays;
import java.util.List;

/**
 * A producer of a topic of the content stream: it buffers messages and publishes them by position.
 */
public interface ContentStreamProducer extends AutoCloseable {

    /**
     * A new message builder.
     *
     * @return the builder
     */
    ContentStreamBuffer.Builder builder();

    /**
     * Buffers a copy of an existing message.
     *
     * @param buffer the message
     * @return this producer
     */
    ContentStreamProducer copy(ContentStreamBuffer buffer);

    /**
     * Buffers the message a builder builds.
     *
     * @param bufferBuilder the builder
     * @return this producer
     */
    ContentStreamProducer produce(ContentStreamBuffer.Builder bufferBuilder);

    /**
     * Buffers the messages of builders and publishes their positions.
     *
     * @param builders the builders
     * @throws ClosedContentStreamException if the producer is closed
     */
    default void publishBuilders(ContentStreamBuffer.Builder... builders) throws ClosedContentStreamException {
        for (ContentStreamBuffer.Builder builder : builders) {
            produce(builder);
        }
        publish(Arrays.stream(builders).map(ContentStreamBuffer.Builder::position).toList());
    }

    /**
     * Publishes the buffered messages of positions.
     *
     * @param positions the positions
     * @throws ClosedContentStreamException if the producer is closed
     */
    default void publish(List<String> positions) throws ClosedContentStreamException {
        publish(positions.toArray(new String[positions.size()]));
    }

    /**
     * Publishes the buffered messages of positions.
     *
     * @param position the positions
     */
    void publish(String... position);

    /**
     * Closes it, without a checked exception.
     */
    @Override
    void close();
}
