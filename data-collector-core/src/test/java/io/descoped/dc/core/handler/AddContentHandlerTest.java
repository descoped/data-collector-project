package io.descoped.dc.core.handler;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.sequence;
import static io.descoped.dc.api.Builders.xpath;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TestServerExtension.class)
class AddContentHandlerTest {

    @Inject
    TestServer testServer;

    @Test
    void thatStoredStateIsEvaluatedForEachEntry() {
        RecordingContentStore contentStore = new RecordingContentStore(
                ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of()));

        Worker.newBuilder()
                .specification(Specification.start("test", "page", "page")
                        .configure(context().topic("topic"))
                        .function(get("page")
                                .url(testServer.testURL("/api/events?position=1&pageSize=3"))
                                .pipe(sequence(xpath("/feed/entry"))
                                        .expected(xpath("/entry/id")))
                                .pipe(parallel(xpath("/feed/entry"))
                                        .variable("position", xpath("/entry/id"))
                                        .pipe(addContent("${position}", "entry")
                                                .storeState("${'key-' + position}", "${position}")
                                                .storeState("constant", 10))
                                        .pipe(publish("${position}")))))
                .contentStore(contentStore)
                .header("Accept", "application/xml")
                .build()
                .run();

        assertEquals(Set.of("1", "2", "3"), contentStore.stateByPosition.keySet());
        assertEquals(Map.of("key-2", "2", "constant", 10), contentStore.stateByPosition.get("2"));
    }

    /*
     * Records the state each entry is buffered with, and leaves the rest to the discarding store.
     */
    static class RecordingContentStore implements ContentStore {

        final ContentStore delegate;
        final Map<String, Map<String, Object>> stateByPosition = new ConcurrentHashMap<>();

        RecordingContentStore(ContentStore delegate) {
            this.delegate = delegate;
        }

        @Override
        public void lock(String topic) {
            delegate.lock(topic);
        }

        @Override
        public void unlock(String topic) {
            delegate.unlock(topic);
        }

        @Override
        public ContentStream contentStream() {
            return delegate.contentStream();
        }

        @Override
        public String lastPosition(String topic) {
            return delegate.lastPosition(topic);
        }

        @Override
        public Set<String> contentKeys(String topic, String position) {
            return delegate.contentKeys(topic, position);
        }

        @Override
        public void addPaginationDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
            delegate.addPaginationDocument(topic, position, contentKey, content, httpRequestInfo);
        }

        @Override
        public void bufferPaginationEntryDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo, Map<String, Object> state) {
            stateByPosition.put(position, Map.copyOf(state));
            delegate.bufferPaginationEntryDocument(topic, position, contentKey, content, httpRequestInfo, state);
        }

        @Override
        public void bufferDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
            delegate.bufferDocument(topic, position, contentKey, content, httpRequestInfo);
        }

        @Override
        public void publish(String topic, String... position) {
            delegate.publish(topic, position);
        }

        @Override
        public HealthContentStreamMonitor monitor() {
            return delegate.monitor();
        }

        @Override
        public void closeTopic(String topic) {
            delegate.closeTopic(topic);
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public void close() {
            try {
                delegate.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
