package io.descoped.dc.server.integrity;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.server.content.ContentStoreComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegrityCheckServiceTest {

    // requests for one topic that race: exactly one starts a job, and the topic is free again when it ends (#67)
    @Test
    void thatOnlyOneJobStartsForATopic() throws Exception {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("content.stream.connector", "rawdata")
                .value("rawdata.client.provider", "memory")
                // an empty topic: the job waits this long for a first message, then ends
                .value("data.collector.integrityCheck.consumer.timeoutInSeconds", "3")
                .value("data.collector.integrityCheck.dbSizeInMb", "10")
                .value("data.collector.integrityCheck.database.location", "./target/integrity-check-service-test")
                .build();
        ContentStoreComponent contentStoreComponent = ContentStoreComponent.create(configuration);
        try {
            IntegrityCheckService service = new IntegrityCheckService(configuration, contentStoreComponent);
            int requests = 8;
            CountDownLatch start = new CountDownLatch(1);
            List<CompletableFuture<Boolean>> started = new ArrayList<>();
            try (ExecutorService executor = Executors.newFixedThreadPool(requests)) {
                for (int i = 0; i < requests; i++) {
                    started.add(CompletableFuture.supplyAsync(() -> {
                        try {
                            start.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return service.createJob("empty-topic");
                    }, executor));
                }
                start.countDown();
            }
            assertEquals(1, started.stream().filter(CompletableFuture::join).count());
            assertTrue(service.isJobRunning("empty-topic"));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            while (service.isJobRunning("empty-topic") && System.nanoTime() < deadline) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
            }
            assertFalse(service.isJobRunning("empty-topic"));
            assertTrue(service.createJob("empty-topic"));
            service.stop();
        } finally {
            contentStoreComponent.close();
        }
    }
}
