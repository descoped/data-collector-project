package io.descoped.dc.content.provider.discarding;

import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamProducer;

public class DiscardingContentStreamProducer implements ContentStreamProducer {

    @Override
    public ContentStreamBuffer.Builder builder() {
        return new DiscardingContentStreamBuffer.Builder();
    }

    @Override
    public ContentStreamProducer copy(ContentStreamBuffer buffer) {
        return this;
    }

    @Override
    public ContentStreamProducer produce(ContentStreamBuffer.Builder bufferBuilder) {
        return this;
    }

    @Override
    public void publish(String... position) {
        // the content is discarded
    }

    @Override
    public void close() throws Exception {
        // nothing to close
    }
}
