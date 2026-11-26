package io.descoped.dc.content.provider.rawdata;

import io.descoped.dc.api.content.ClosedContentStreamException;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.content.ContentStreamCursor;
import io.descoped.dc.api.content.ContentStreamProducer;
import io.descoped.rawdata.api.RawdataClient;
import io.descoped.rawdata.api.RawdataMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A content stream on a rawdata-client topic: producers publish content as rawdata messages, optionally encrypted.
 */
public class RawdataClientContentStream implements ContentStream {

    private final RawdataClient client;
    private final Function<byte[], byte[]> tryEncryptContent;
    private final Map<String, RawdataClientContentStreamProducer> producerMap = new ConcurrentHashMap<>();
    private final Map<String, RawdataClientContentStreamConsumer> consumerMap = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a stream that writes content as given.
     *
     * @param client the rawdata client
     */
    public RawdataClientContentStream(RawdataClient client) {
        this(client, null);
    }

    /**
     * Creates a stream whose producers pass content and manifests through {@code tryEncryptContent}.
     *
     * @param client            the rawdata client
     * @param tryEncryptContent encrypts content, or returns it as given; {@code null} for none
     */
    public RawdataClientContentStream(RawdataClient client, Function<byte[], byte[]> tryEncryptContent) {
        this.client = client;
        this.tryEncryptContent = tryEncryptContent;
    }

    @Override
    public String lastPosition(String topic) {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        RawdataMessage message = client.lastMessage(topic);
        return message != null ? message.position() : null;
    }

    @Override
    public ContentStreamBuffer lastMessage(String topic) {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        RawdataMessage message = client.lastMessage(topic);
        return message != null ? RawdataClientContentStreamBuffer.of(message) : null;
    }

    @Override
    public ContentStreamProducer producer(String topic) {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        return producerMap.computeIfAbsent(topic, p -> new RawdataClientContentStreamProducer(client.producer(topic), this::closeAndRemoveProducer, tryEncryptContent));
    }

    @Override
    public ContentStreamConsumer consumer(String topic, ContentStreamCursor cursor) {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        return consumerMap.computeIfAbsent(topic, c -> new RawdataClientContentStreamConsumer(client.consumer(topic), this::closeAndRemoveConsumer));
    }

    @Override
    public void closeAndRemoveProducer(String topic) {
        RawdataClientContentStreamProducer producer = producerMap.remove(topic);
        if (producer != null) {
            producer.close();
        }
    }

    @Override
    public void closeAndRemoveConsumer(String topic) {
        RawdataClientContentStreamConsumer consumer = consumerMap.remove(topic);
        if (consumer != null) {
            consumer.close();
        }
    }

    /**
     * Whether the stream is closed.
     *
     * @return true after {@link #close()}
     */
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (RawdataClientContentStreamProducer producer : producerMap.values()) {
                producer.close();
            }
            for (RawdataClientContentStreamConsumer consumer : consumerMap.values()) {
                consumer.close();
            }
            producerMap.clear();
            consumerMap.clear();
            client.close();
        }
    }
}
