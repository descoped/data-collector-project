package io.descoped.dc.content.provider.discarding;

import io.descoped.dc.api.content.ClosedContentStreamException;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;

import java.util.concurrent.TimeUnit;

/**
 * A consumer that receives nothing.
 */
public class DiscardingContentStreamConsumer implements ContentStreamConsumer {

    /**
     * Creates the consumer.
     */
    public DiscardingContentStreamConsumer() {
        // no state
    }

    @Override
    public String topic() {
        return null;
    }

    @Override
    public ContentStreamBuffer receive(int timeout, TimeUnit unit) throws InterruptedException, ClosedContentStreamException {
        return null;
    }

    @Override
    public void seek(long timestamp) {
        // nothing to seek in
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        // nothing to close
    }
}
