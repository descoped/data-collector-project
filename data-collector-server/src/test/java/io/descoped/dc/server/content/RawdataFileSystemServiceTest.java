package io.descoped.dc.server.content;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStreamProducer;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.server.TestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RawdataFileSystemServiceTest {

    static int EXPECTED_MESSAGES = 100;

    static DynamicConfiguration configuration = DynamicConfiguration.builder()
            .value("content.stream.connector", "rawdata")
            .value("rawdata.client.provider", "memory")
            .value("data.collector.rawdata.dump.enabled", "true")
            .value("data.collector.rawdata.dump.location", CommonUtils.currentPath().resolve("target").resolve("storage").toFile().getAbsolutePath())
            .value("data.collector.rawdata.dump.topic", "test-stream")
            .value("data.collector.consumer.timeoutInSeconds", "1")
            .build();
    static TestServer server;
    static TestClient client;

    @BeforeAll
    static void beforeAll() {
        server = TestServer.create(configuration);
        client = TestClient.create(server);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    // the API's close() is declared to throw Exception, which javac's try lint reports
    static void produceMessages(ContentStoreComponent contentStoreComponent) {
        ContentStore contentStore = contentStoreComponent.getDelegate();
        try (ContentStreamProducer producer = contentStore.contentStream().producer(configuration.find("data.collector.rawdata.dump.topic").orElse(null))) {
            for (int n = 0; n < EXPECTED_MESSAGES; n++) {
                producer.publishBuilders(producer.builder().position(String.valueOf(n)).put("entry", "DATA".getBytes(StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void exportData() {
        ContentStoreComponent contentStoreComponent = server.getApplication().unwrap(ContentStoreComponent.class);
        CompletableFuture<Void> producerFuture = CompletableFuture.runAsync(() -> produceMessages(contentStoreComponent));

        RawdataFileSystemService service = server.getApplication().unwrap(RawdataFileSystemService.class);

        // wait at most 30 seconds for the exporter, so a missing export fails instead of hanging
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (countWorkDirFolders(service.getWorkDir()) != EXPECTED_MESSAGES && System.nanoTime() < deadline) {
            nap();
        }
        producerFuture.join();

        assertEquals(EXPECTED_MESSAGES, countWorkDirFolders(service.getWorkDir()));
    }

    private long countWorkDirFolders(Path workDir) {
        try (Stream<Path> paths = Files.list(workDir)) {
            return paths.filter(p -> p.toFile().isDirectory()).count();
        } catch (NoSuchFileException e) {
            return 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void nap() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
