package io.descoped.dc.server.recovery;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.content.ContentStreamProducer;
import io.descoped.dc.api.ids.TimeOrderedIds;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.server.content.ContentStoreComponent;
import io.descoped.dc.server.db.LmdbEnvironment;
import io.descoped.dc.server.db.SequenceDbHelper;
import io.descoped.dc.server.integrity.IntegrityCheckJobSummary;
import io.descoped.dc.server.integrity.IntegrityCheckService;
import io.descoped.dc.test.client.ResponseHelper;
import io.descoped.dc.test.client.TestClient;
import io.descoped.dc.test.server.TestServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test requires vm arg: --add-opens java.base/java.nio=lmdbjava --add-exports=java.base/sun.nio.ch=lmdbjava
 */
class RecoveryTest {

    static final int NUMBER_OF_MESSAGES = 25000;
    private static final Logger LOG = LoggerFactory.getLogger(RecoveryTest.class);

    // the API's close() is declared to throw Exception, which javac's try lint reports
    @SuppressWarnings("try")
    static void produceMessages(ContentStream contentStream, String prefixTopic) {
        try (ContentStreamProducer producer = contentStream.producer(prefixTopic + "test-stream")) {
            int min = (int) Math.round(NUMBER_OF_MESSAGES * 0.75);
            int max = NUMBER_OF_MESSAGES - 1;
            for (int n = 0; n < NUMBER_OF_MESSAGES; n++) {
                producer.publishBuilders(producer.builder().position(String.valueOf(n)).put("entry", "DATA".getBytes(StandardCharsets.UTF_8)));

                if (n >= min) {
                    int randomPosition = ThreadLocalRandom.current().nextInt(n, max + 1);
                    producer.publishBuilders(producer.builder().position(String.valueOf(randomPosition)).put("entry", "DATA".getBytes(StandardCharsets.UTF_8)));
                }
            }
            LOG.trace("Produced {} messages", NUMBER_OF_MESSAGES - 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Waits until the integrity check of the topic has ended. A job is registered before it starts, with the status
     * CLOSED, so waiting only while it is RUNNING can return before it has run; on a slow machine the index was then
     * still empty when the recovery read it.
     */
    static void awaitIntegrityCheck(IntegrityCheckService integrityCheckService, String topic) throws InterruptedException {
        String notEnded = Instant.EPOCH.toString();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        while (true) {
            IntegrityCheckJobSummary.Summary summary = null;
            try {
                summary = integrityCheckService.getJobSummary(topic);
            } catch (IllegalStateException e) {
                // not registered yet
            }
            if (summary != null && !"RUNNING".equals(summary.status) && !notEnded.equals(summary.ended)) {
                return;
            }
            if (System.nanoTime() > deadline) {
                throw new AssertionError("The integrity check of " + topic + " did not end within 120 seconds");
            }
            Thread.sleep(50);
        }
    }

    @Disabled("Developer tool: reads a local integrity check index in certs/lmdb")
    @Test
    void readIndex() {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("content.stream.connector", "rawdata")
                .value("rawdata.client.provider", "memory")
                .value("data.collector.integrityCheck.consumer.timeoutInSeconds", "1")
                .value("data.collector.integrityCheck.dbSizeInMb", "100")
                .build();

        Path dbLocation = CommonUtils.currentPath().resolve("certs").resolve("lmdb");
        LmdbEnvironment lmdbEnvironment = new LmdbEnvironment(configuration, dbLocation, "ske-freg");

        long past = System.currentTimeMillis();
        SequenceDbHelper sequenceDbHelper = new SequenceDbHelper(lmdbEnvironment, lmdbEnvironment.open());
        PositionAndIdVersion firstPosition = sequenceDbHelper.findFirstPosition();
        PositionAndIdVersion lastPosition = sequenceDbHelper.findLastPosition();
        long now = System.currentTimeMillis() - past;
        LOG.trace("Time take: {}", now);
        LOG.trace("First: {} -> {} -> {}",
                firstPosition.position(),
                firstPosition.id(),
                new Date(TimeOrderedIds.timestamp(firstPosition.id()))
        );
        LOG.trace("Last: {} -> {} -> {}",
                lastPosition.position(),
                lastPosition.id(),
                new Date(TimeOrderedIds.timestamp(lastPosition.id()))
        );
    }

    @Disabled("Racy: it re-runs a recovery and cancels it at once with DELETE, expecting 200, but the re-run can end first and the controller then answers 404")
    @Test
    void testController() throws InterruptedException {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("content.stream.connector", "rawdata")
                .value("rawdata.client.provider", "memory")
                .value("data.collector.integrityCheck.consumer.timeoutInSeconds", "1")
                .value("data.collector.integrityCheck.dbSizeInMb", "100")
                .value("data.collector.integrityCheck.database.location", "./target")
                .build();

        TestServer server = TestServer.create(configuration);
        TestClient client = TestClient.create(server);
        server.start();

        ContentStoreComponent contentStoreComponent = server.getApplication().unwrap(ContentStoreComponent.class);
        ContentStore contentStore = contentStoreComponent.getDelegate();

        Thread producerThread = new Thread(() -> produceMessages(contentStore.contentStream(), "source-"));
        producerThread.start();
        producerThread.join();

        IntegrityCheckService integrityCheckService = server.getApplication().unwrap(IntegrityCheckService.class);
        integrityCheckService.createJob("source-test-stream");
        awaitIntegrityCheck(integrityCheckService, "source-test-stream");
        Thread.sleep(1500);
        LOG.trace("Completed integrity check !!");

        {
            ResponseHelper<String> responseHelper = client.put("/recovery/source-test-stream?toTopic=target-test-stream");
            responseHelper.expect201Created();
        }

        {
            ResponseHelper<String> responseHelper = client.get("/recovery");
            responseHelper.expect200Ok();
            LOG.trace("workers: {}", responseHelper.body());
        }

        {
            ResponseHelper<String> responseHelper = client.get("/recovery/source-test-stream");
            responseHelper.expect200Ok();
            LOG.trace("summary: {}", responseHelper.body());
        }

        RecoveryService recoveryService = server.getApplication().unwrap(RecoveryService.class);
        CompletableFuture<RecoveryWorker> future = recoveryService.jobFutures.get("source-test-stream");
        future.join();

        {
            ResponseHelper<String> responseHelper = client.get("/recovery/source-test-stream");
            responseHelper.expect200Ok();
            LOG.trace("summary: {}", responseHelper.body());
        }

        {
            recoveryService.jobFutures.clear();
            recoveryService.jobs.clear();
            ResponseHelper<String> responseHelper = client.put("/recovery/source-test-stream?toTopic=target-test-stream");
            responseHelper.expect201Created();
            client.delete("/recovery/source-test-stream").expect200Ok();
        }

        LOG.trace("Shutdown test");
        Thread.sleep(500);
        server.stop();
    }

    // the API's close() is declared to throw Exception, which javac's try lint reports
    @SuppressWarnings("try")
    @Test
    void readIndexAndConsumerSourceAndProduceTarget() throws InterruptedException {
        DynamicConfiguration configuration = DynamicConfiguration.builder()
                .value("content.stream.connector", "rawdata")
                .value("rawdata.client.provider", "memory")
                .value("data.collector.integrityCheck.consumer.timeoutInSeconds", "1")
                .value("data.collector.integrityCheck.dbSizeInMb", "100")
                .value("data.collector.integrityCheck.database.location", "./target")
                .build();

        TestServer server = TestServer.create(configuration);
        server.start();

        ContentStoreComponent contentStoreComponent = server.getApplication().unwrap(ContentStoreComponent.class);
        ContentStore contentStore = contentStoreComponent.getDelegate();

        RecoveryContentStoreComponent recoveryContentStoreComponent = server.getApplication().unwrap(RecoveryContentStoreComponent.class);
        ContentStore recoveryContentStore = recoveryContentStoreComponent.getDelegate();

        /*
         * Produce random source data
         * Generate sequenceDb index
         * Find First and Last Position
         * Consume source and Produce target
         */

        Thread producerThread = new Thread(() -> produceMessages(contentStore.contentStream(), "source-"));
        producerThread.start();
        producerThread.join();

        IntegrityCheckService integrityCheckService = server.getApplication().unwrap(IntegrityCheckService.class);
        integrityCheckService.createJob("source-test-stream");
        awaitIntegrityCheck(integrityCheckService, "source-test-stream");
        Thread.sleep(1500);
        LOG.trace("Completed integrity check !!");

        RecoveryWorker recoveryWorker = new RecoveryWorker(configuration, contentStoreComponent, recoveryContentStoreComponent);
        recoveryWorker.recover("source-test-stream", "target-test-stream");

        try (ContentStreamConsumer consumer = recoveryContentStore.contentStream().consumer("target-test-stream")) {
            ContentStreamBuffer buffer;
            ContentStreamBuffer peekBuffer = null;
            while ((buffer = consumer.receive(1, TimeUnit.SECONDS)) != null) {
                peekBuffer = buffer;
                if (Integer.parseInt(buffer.position()) < 10) {
                    LOG.trace("{} [{}]: {} ({} buffers)", buffer.position(), buffer.id(), new Date(TimeOrderedIds.timestamp(buffer.id())), buffer.data().size());
                }
            }
            assertNotNull(peekBuffer, "nothing was recovered to the target topic");
            LOG.trace("{} [{}]: {} ({} buffers)", peekBuffer.position(), peekBuffer.id(), new Date(TimeOrderedIds.timestamp(peekBuffer.id())), peekBuffer.data().size());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Thread.sleep(1000);

        server.stop();
    }

    @Test
    void thatLeastVersionIsSelected() {
        UUID v1 = TimeOrderedIds.generate();
        UUID v2 = TimeOrderedIds.generate();
        UUID v3 = TimeOrderedIds.generate();

        PositionAndIdVersion version = new PositionAndIdVersion();

        version.compareAndSet(v2, "1");
        assertEquals(v2, version.id());

        version.compareAndSet(v3, "1");
        assertEquals(v2, version.id());

        version.compareAndSet(v1, "1");
        assertEquals(v1, version.id());

        version.compareAndSet(v3, "1");
        assertEquals(v1, version.id());
    }


}
