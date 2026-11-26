package io.descoped.dc.content.provider.rawdata;

import io.descoped.dc.api.content.ClosedContentStreamException;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.rawdata.api.RawdataConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A consumer of a rawdata-client topic.
 */
public class RawdataClientContentStreamConsumer implements ContentStreamConsumer {

    private final RawdataConsumer consumer;
    private final Consumer<String> closeAndRemoveConsumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates the consumer.
     *
     * @param consumer               the rawdata consumer
     * @param closeAndRemoveConsumer called with the topic when the consumer is closed
     */
    public RawdataClientContentStreamConsumer(RawdataConsumer consumer, Consumer<String> closeAndRemoveConsumer) {
        this.consumer = consumer;
        this.closeAndRemoveConsumer = closeAndRemoveConsumer;
    }

    @Override
    public String topic() {
        return consumer.topic();
    }

    @Override
    public ContentStreamBuffer receive(int timeout, TimeUnit unit) throws InterruptedException, ClosedContentStreamException {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        return RawdataClientContentStreamBuffer.of(consumer.receive(timeout, unit));
    }

    @Override
    public void seek(long timestamp) {
        if (isClosed()) {
            throw new ClosedContentStreamException();
        }
        consumer.seek(timestamp);
    }

    @Override
    public boolean isClosed() {
        return consumer.isClosed();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeAndRemoveConsumer.accept(consumer.topic());
            consumer.close();
        }
    }
}
