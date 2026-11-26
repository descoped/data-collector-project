package io.descoped.dc.api.content;

import java.util.concurrent.TimeUnit;

/**
 * A consumer of a topic of the content stream.
 */
public interface ContentStreamConsumer extends AutoCloseable {

    /**
     * The topic.
     *
     * @return the topic
     */
    String topic();

    /**
     * Waits for the next message.
     *
     * @param timeout the longest wait
     * @param unit    the unit of the wait
     * @return the message, or {@code null} if none arrives in time
     * @throws InterruptedException         if the thread is interrupted while waiting
     * @throws ClosedContentStreamException if the consumer is closed
     */
    ContentStreamBuffer receive(int timeout, TimeUnit unit) throws InterruptedException, ClosedContentStreamException;

    /**
     * Moves the consumer to a time in the topic.
     *
     * @param timestamp the time, in milliseconds since the epoch
     */
    void seek(long timestamp);

    /**
     * Whether the consumer is closed.
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
