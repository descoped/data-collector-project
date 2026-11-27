package io.descoped.dc.server.content;

import io.descoped.dc.api.CorrelationIds;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Headers;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * A worker that is stopped can close its topic on a thread whose interrupt flag is set, as Worker.run's finally does
 * when the wait for its pool was interrupted: the topic's producer is closed and uploads what it holds, and the flag is
 * kept (rawdata-client 3.2.0-SNAPSHOT build 3 calls the store on an interrupted thread like on any other).
 */
class InterruptedThreadTest {

    @Test
    void thatAnInterruptedThreadClosesTheTopicAndUploadsIt() throws Exception {
        Path root = Path.of("target", "interrupted-thread-test", UUID.randomUUID().toString()).toAbsolutePath();
        Path store = Files.createDirectories(root.resolve("store"));
        Map<String, String> configuration = CrashResumeTest.configuration(store, Files.createDirectories(root.resolve("temp")));
        HttpRequestInfo httpRequestInfo = new HttpRequestInfo(CorrelationIds.create(ExecutionContext.empty()), null, -1, new Headers(), new Headers(), -1);

        try (ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", configuration)) {
            for (String position : List.of("1", "2", "3")) {
                contentStore.bufferDocument("interrupted", position, "entry", position.getBytes(StandardCharsets.UTF_8), httpRequestInfo);
                contentStore.publish("interrupted", position);
            }

            Thread.currentThread().interrupt();
            try {
                contentStore.closeTopic("interrupted");
                assertTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is kept");
            } finally {
                Thread.interrupted();
            }
        }

        // a reader with a buffer folder of its own sees only what the closed topic uploaded
        Map<String, String> reading = CrashResumeTest.configuration(store, Files.createDirectories(root.resolve("temp-reader")));
        try (ContentStore reader = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", reading)) {
            assertEquals(List.of(1L, 2L, 3L), CrashResumeTest.positions(reader, "interrupted"));
        }
    }
}
