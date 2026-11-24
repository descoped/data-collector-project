package io.descoped.dc.content.provider.discarding;

import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.content.ContentStreamCursor;
import io.descoped.dc.api.content.ContentStreamProducer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscardingContentStream implements ContentStream {

    final Map<String, ContentStreamBuffer> buffersByName = new ConcurrentHashMap<>();

    @Override
    public String lastPosition(String topic) {
        return null;
    }

    @Override
    public ContentStreamBuffer lastMessage(String topic) {
        return null;
    }

    @Override
    public ContentStreamProducer producer(String topic) {
        return new DiscardingContentStreamProducer();
    }

    @Override
    public ContentStreamConsumer consumer(String topic, ContentStreamCursor cursor) {
        return new DiscardingContentStreamConsumer();
    }

    @Override
    public void closeAndRemoveProducer(String topic) {
        // no producers are kept
    }

    @Override
    public void closeAndRemoveConsumer(String topic) {
        // no consumers are kept
    }

    @Override
    public void close() throws Exception {
        // nothing to close
    }
}
