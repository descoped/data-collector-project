package io.descoped.dc.core.content;

import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Headers;
import io.descoped.service.provider.api.ProviderConfigurator;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContentStoreExporterTest {

    @BeforeAll
    static void beforeAll() {
        ContentStoreAgent.install(ByteBuddyAgent.install());
    }

    @AfterAll
    static void afterAll() throws IOException {
        StringWriter sw = new StringWriter();
        TextFormat.write004(sw, CollectorRegistry.defaultRegistry.metricFamilySamples());
        System.out.printf("%s%n", sw);
    }

    @Test
    void name() {
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", Map.of("rawdata.client.provider", "memory"));
        assertNotNull(contentStore);
        double positions = sample("dc_content_stream_pagination_entry_publish_position_count_total", "topic");
        HttpRequestInfo httpRequestInfo = new HttpRequestInfo(CorrelationIds.create(ExecutionContext.empty()), null, -1, new Headers(), new Headers(), -1);
        contentStore.addPaginationDocument("topic", "1", "page", "PAGINATION_PAGE".getBytes(), httpRequestInfo);
        contentStore.bufferPaginationEntryDocument("topic", "1", "page", "PAGINATION_PAGE_ENTRY".getBytes(), httpRequestInfo, new LinkedHashMap<>());
        contentStore.bufferDocument("topic", "1", "entry", "PAYLOAD".getBytes(), httpRequestInfo);
        assertEquals(2, contentStore.contentKeys("topic", "1").size());
        contentStore.publish("topic", "1");
        // the agent inlined the advice into the rawdata connector's store (issue #25)
        assertEquals(positions + 1, sample("dc_content_stream_pagination_entry_publish_position_count_total", "topic"));
    }

    static double sample(String name, String topic) {
        Double value = CollectorRegistry.defaultRegistry.getSampleValue(name, new String[]{"topic"}, new String[]{topic});
        return value == null ? 0 : value;
    }

    @Test
    void thatInterceptorsCountContentStoreWrites() throws Exception {
        String topic = "interceptor-topic";
        double pages = sample("dc_content_stream_pagination_page_publish_count_total", topic);
        double entries = sample("dc_content_stream_buffer_pagination_entry_count_total", topic);
        double documents = sample("dc_content_stream_buffer_entry_document_count_total", topic);
        double positions = sample("dc_content_stream_pagination_entry_publish_position_count_total", topic);

        ContentStoreExporter.Paginate.exit(ContentStoreExporter.Paginate.enter(topic), topic);
        ContentStoreExporter.Entry.exit(topic, null);
        ContentStoreExporter.Entry.exit(topic, new IllegalStateException("not buffered"));
        ContentStoreExporter.Document.exit(topic, null);
        ContentStoreExporter.Publish.exit(ContentStoreExporter.Publish.enter(topic), topic, new String[]{"1", "2"});

        assertEquals(pages + 1, sample("dc_content_stream_pagination_page_publish_count_total", topic));
        assertEquals(entries + 1, sample("dc_content_stream_buffer_pagination_entry_count_total", topic));
        assertEquals(documents + 1, sample("dc_content_stream_buffer_entry_document_count_total", topic));
        assertEquals(positions + 2, sample("dc_content_stream_pagination_entry_publish_position_count_total", topic));
    }
}
