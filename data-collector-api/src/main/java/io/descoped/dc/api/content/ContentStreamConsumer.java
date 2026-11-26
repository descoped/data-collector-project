package io.descoped.dc.api.content;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("try") // close() keeps AutoCloseable's throws Exception; narrowing it is the owner's decision
public interface ContentStreamConsumer extends AutoCloseable {

    String topic();

    ContentStreamBuffer receive(int timeout, TimeUnit unit) throws InterruptedException, ClosedContentStreamException;

    void seek(long timestamp);

    boolean isClosed();

}
