package io.descoped.dc.core.handler;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.execute;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.jqpath;
import static io.descoped.dc.api.Builders.jsonToken;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.sequence;
import static io.descoped.dc.api.Builders.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * The feed is streamed from a file while each entry's own request runs: the entry's request must not take over the
 * feed's file body handler, or it would truncate the file the feed is read from (#47).
 */
@ExtendWith(TestServerExtension.class)
class NestedRequestBodyHandlerTest {

    @Inject
    TestServer testServer;

    @Test
    void thatANestedRequestKeepsOutOfTheFeedsFile() {
        DocumentRecordingContentStore contentStore = new DocumentRecordingContentStore(
                ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of()));

        Worker.newBuilder()
                .specification(Specification.start("test", "page", "page")
                        .configure(context().topic("topic"))
                        .function(get("page")
                                .url(testServer.testURL("/api/events?position=1&pageSize=10"))
                                .pipe(sequence(jsonToken())
                                        .expected(jqpath(".id")))
                                .pipe(parallel(jsonToken())
                                        .variable("position", jqpath(".id"))
                                        .pipe(addContent("${position}", "entry"))
                                        .pipe(execute("event-doc")
                                                .inputVariable("eventId", jqpath(".[\"event-id\"]")))
                                        .pipe(publish("${position}"))))
                        .function(get("event-doc")
                                .url(testServer.testURL("/api/events/${eventId}?type=event"))
                                .validate(status().success(200, 299))
                                .pipe(addContent("${position}", "event-doc"))))
                .contentStore(contentStore)
                .header("Accept", "application/json")
                .build()
                .run();

        assertEquals("10", contentStore.lastPosition("topic"));
        // each entry's own response is its body; with the feed's file handler it would be written to that file instead
        assertEquals(10, contentStore.eventDocumentSizes.size());
        contentStore.eventDocumentSizes.forEach((position, size) -> assertTrue(size > 0, "event-doc of " + position));
    }

    static class DocumentRecordingContentStore extends AddContentHandlerTest.RecordingContentStore {

        final Map<String, Integer> eventDocumentSizes = new ConcurrentHashMap<>();

        DocumentRecordingContentStore(ContentStore delegate) {
            super(delegate);
        }

        @Override
        public void bufferDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
            if ("event-doc".equals(contentKey)) {
                eventDocumentSizes.put(position, content == null ? 0 : content.length);
            }
            super.bufferDocument(topic, position, contentKey, content, httpRequestInfo);
        }
    }
}
