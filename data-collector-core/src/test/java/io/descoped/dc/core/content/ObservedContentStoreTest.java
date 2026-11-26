package io.descoped.dc.core.content;

import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.core.metrics.MetricSamples;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservedContentStoreTest {

    static final HttpRequestInfo HTTP_REQUEST_INFO = new HttpRequestInfo(CorrelationIds.create(ExecutionContext.empty()), null, -1, new Headers(), new Headers(), -1);

    @AfterAll
    static void afterAll() {
        System.out.printf("%s%n", MetricSamples.text());
    }

    static double sample(String name, String topic) {
        return MetricSamples.value(name, new String[]{"topic"}, new String[]{topic});
    }

    static ContentStore rawdataStore() {
        return ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", Map.of("rawdata.client.provider", "memory"));
    }

    @Test
    void thatTheWritesOfAStoreAreCounted() {
        String topic = "observed-topic";
        double pages = sample("dc_content_stream_pagination_page_publish_count_total", topic);
        double entries = sample("dc_content_stream_buffer_pagination_entry_count_total", topic);
        double documents = sample("dc_content_stream_buffer_entry_document_count_total", topic);
        double positions = sample("dc_content_stream_pagination_entry_publish_position_count_total", topic);

        try (ContentStore contentStore = ObservedContentStore.of(rawdataStore())) {
            contentStore.addPaginationDocument(topic, "1", "page", "PAGINATION_PAGE".getBytes(), HTTP_REQUEST_INFO);
            contentStore.bufferPaginationEntryDocument(topic, "1", "page", "PAGINATION_PAGE_ENTRY".getBytes(), HTTP_REQUEST_INFO, new LinkedHashMap<>());
            contentStore.bufferDocument(topic, "1", "entry", "PAYLOAD".getBytes(), HTTP_REQUEST_INFO);
            assertEquals(2, contentStore.contentKeys(topic, "1").size());
            contentStore.bufferDocument(topic, "2", "entry", "PAYLOAD".getBytes(), HTTP_REQUEST_INFO);
            contentStore.publish(topic, "1", "2");
        }

        assertEquals(pages + 1, sample("dc_content_stream_pagination_page_publish_count_total", topic));
        assertEquals(entries + 1, sample("dc_content_stream_buffer_pagination_entry_count_total", topic));
        assertEquals(documents + 2, sample("dc_content_stream_buffer_entry_document_count_total", topic));
        assertEquals(positions + 2, sample("dc_content_stream_pagination_entry_publish_position_count_total", topic));
    }

    // as in 2.x: a page and the positions are counted when writing them fails, a buffered entry is not
    @Test
    void thatFailedWritesAreCountedAs2xDid() {
        String topic = "failing-topic";
        double pages = sample("dc_content_stream_pagination_page_publish_count_total", topic);
        double entries = sample("dc_content_stream_buffer_pagination_entry_count_total", topic);

        ContentStore contentStore = ObservedContentStore.of(rawdataStore());
        contentStore.close();
        byte[] page = "PAGE".getBytes();
        byte[] entry = "ENTRY".getBytes();
        Map<String, Object> state = new LinkedHashMap<>();
        assertThrows(RuntimeException.class, () -> contentStore.addPaginationDocument(topic, "1", "page", page, HTTP_REQUEST_INFO));
        assertThrows(RuntimeException.class, () -> contentStore.bufferPaginationEntryDocument(topic, "1", "page", entry, HTTP_REQUEST_INFO, state));

        assertEquals(pages + 1, sample("dc_content_stream_pagination_page_publish_count_total", topic));
        assertEquals(entries, sample("dc_content_stream_buffer_pagination_entry_count_total", topic));
    }

    @Test
    void thatAStoreIsObservedOnce() {
        try (ContentStore contentStore = ObservedContentStore.of(rawdataStore())) {
            assertSame(contentStore, ObservedContentStore.of(contentStore));
        }
    }
}
