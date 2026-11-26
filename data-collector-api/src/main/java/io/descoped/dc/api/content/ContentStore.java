package io.descoped.dc.api.content;

import java.util.Map;
import java.util.Set;

/**
 * Where the engine buffers each position's documents and publishes them to a topic of the content stream. A
 * content store provider implements it.
 */
public interface ContentStore extends AutoCloseable {

    /**
     * Locks a topic for the calling thread.
     *
     * @param topic the topic
     */
    void lock(String topic);

    /**
     * Unlocks a topic the calling thread locked.
     *
     * @param topic the topic
     */
    void unlock(String topic);

    /**
     * The content stream the store publishes to.
     *
     * @return the content stream
     */
    ContentStream contentStream();

    /**
     * The position of the last message published to a topic.
     *
     * @param topic the topic
     * @return the position, or {@code null} if the topic has none
     */
    String lastPosition(String topic);

    /**
     * The content keys buffered for a position.
     *
     * @param topic    the topic
     * @param position the position
     * @return the content keys, empty if nothing is buffered
     */
    Set<String> contentKeys(String topic, String position);

    /**
     * Publishes a page document at once, to the topic's page topic.
     *
     * @param topic           the topic
     * @param position        the page's position
     * @param contentKey      the content key
     * @param content         the content
     * @param httpRequestInfo the request that fetched the page
     */
    void addPaginationDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo);

    /**
     * Buffers a page entry's document under its position until the position is published.
     *
     * @param topic           the topic
     * @param position        the position
     * @param contentKey      the content key
     * @param content         the content
     * @param httpRequestInfo the request that fetched the content
     * @param state           the state stored with the content
     */
    void bufferPaginationEntryDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo, Map<String, Object> state);

    /**
     * Buffers a document under its position until the position is published.
     *
     * @param topic           the topic
     * @param position        the position
     * @param contentKey      the content key
     * @param content         the content
     * @param httpRequestInfo the request that fetched the content
     */
    void bufferDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo);

    /**
     * Publishes the buffered content of positions to a topic, in the order given.
     *
     * @param topic    the topic
     * @param position the positions
     */
    void publish(String topic, String... position);

    /**
     * The store's health monitor.
     *
     * @return the monitor
     */
    HealthContentStreamMonitor monitor();

    /**
     * Closes a topic's producer.
     *
     * @param topic the topic
     */
    void closeTopic(String topic);

    /**
     * Whether the store is closed.
     *
     * @return {@code true} once closed
     */
    boolean isClosed();

    /**
     * Closes it, without a checked exception.
     */
    @Override
    void close();
}
