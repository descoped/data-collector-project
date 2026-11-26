package io.descoped.dc.api.content;

/**
 * The stream of topics a content store publishes to and reads from.
 */
public interface ContentStream extends AutoCloseable {

    /**
     * The position of the last message of a topic.
     *
     * @param topic the topic
     * @return the position, or {@code null} if the topic has none
     */
    String lastPosition(String topic);

    /**
     * The last message of a topic.
     *
     * @param topic the topic
     * @return the message, or {@code null} if the topic has none
     */
    ContentStreamBuffer lastMessage(String topic);

    /**
     * The producer of a topic.
     *
     * @param topic the topic
     * @return the producer
     */
    ContentStreamProducer producer(String topic);

    /**
     * A consumer of a topic, without a cursor.
     *
     * @param topic the topic
     * @return the consumer
     */
    default ContentStreamConsumer consumer(String topic) {
        return consumer(topic, null);
    }

    /**
     * A consumer of a topic.
     *
     * @param topic  the topic
     * @param cursor where to start, or {@code null}
     * @return the consumer
     */
    ContentStreamConsumer consumer(String topic, ContentStreamCursor cursor);

    /**
     * Closes a topic's producer and forgets it.
     *
     * @param topic the topic
     */
    void closeAndRemoveProducer(String topic);

    /**
     * Closes a topic's consumer and forgets it.
     *
     * @param topic the topic
     */
    void closeAndRemoveConsumer(String topic);

    /**
     * Closes it, without a checked exception.
     */
    @Override
    void close();
}
