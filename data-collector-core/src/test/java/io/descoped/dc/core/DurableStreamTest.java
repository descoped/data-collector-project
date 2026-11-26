package io.descoped.dc.core;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.node.builder.NodeBuilder;
import io.descoped.dc.api.node.builder.ParallelBuilder;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.LongStream;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.execute;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.nextPage;
import static io.descoped.dc.api.Builders.paginate;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.regex;
import static io.descoped.dc.api.Builders.sequence;
import static io.descoped.dc.api.Builders.status;
import static io.descoped.dc.api.Builders.whenVariableIsNull;
import static io.descoped.dc.api.Builders.xpath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * A stream is durable: a worker that is stopped and resumed, however it resumes, writes every entry once, in order,
 * and every complete page once; a page is committed only when it is complete (#71, #72). Where the stops land does
 * not matter: the result must be the same.
 */
@ExtendWith(TestServerExtension.class)
class DurableStreamTest {

    static final int ENTRIES = 3000;
    static final Map<String, String> CONFIGURATION = Map.of("content.stream.connector", "rawdata", "rawdata.client.provider", "memory");

    @Inject
    TestServer testServer;

    // the page's position is the position of its first entry; the page size is 10
    SpecificationBuilder specification(String resumeExpression, NodeBuilder... entrySteps) {
        return Specification.start("durable", "pages", "loop")
                .configure(context()
                        .topic("durable")
                        .header("accept", "application/xml")
                        .variable("nextPosition", resumeExpression))
                .function(paginate("loop")
                        .variable("fromPosition", "${nextPosition}")
                        .addPageContent("fromPosition")
                        .iterate(execute("page"))
                        .prefetchThreshold(15)
                        .until(whenVariableIsNull("nextPosition")))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=${fromPosition}&pageSize=10&stopAt=" + ENTRIES))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(nextPage()
                                .output("nextPosition", regex(xpath("/feed/link[@rel=\"next\"]/@href"), "(?<=[?&]position=)[^&]*")))
                        .pipe(entry(entrySteps))
                        .returnVariables("nextPosition"));
    }

    static ParallelBuilder entry(NodeBuilder... entrySteps) {
        ParallelBuilder entry = parallel(xpath("/feed/entry")).variable("position", xpath("/entry/id"));
        for (NodeBuilder step : entrySteps) {
            entry.pipe(step);
        }
        return entry.pipe(publish("${position}"));
    }

    Worker worker(String resumeExpression, ContentStore contentStore) {
        return Worker.newBuilder()
                .specification(specification(resumeExpression, addContent("${position}", "entry")))
                .configuration(CONFIGURATION)
                .contentStore(contentStore)
                .keepContentStoreOpenOnWorkerCompletion(true)
                .build();
    }

    void stopAndResume(String resumeExpression) throws Exception {
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", CONFIGURATION);
        try {
            for (int stop = 0; stop < 3; stop++) {
                Worker worker = worker(resumeExpression, contentStore);
                CompletableFuture<?> run = worker.runAsync();
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(300));
                worker.terminate();
                try {
                    run.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException _) {
                    // canceled
                }
                // what the stopped run buffered and did not publish is gone (#72)
                assertEquals(0, contentStore.monitor().build().activePositionCount, "positions left buffered");
            }
            worker(resumeExpression, contentStore).run();

            // the feed gives a whole page from any position before its end, so a run resumed in the middle of a page
            // ends a few entries after it
            List<Long> entries = positions(contentStore, "durable");
            long last = entries.isEmpty() ? 0 : entries.getLast();
            assertTrue(last >= ENTRIES, () -> "the stream reaches its end: " + difference(entries));
            assertEquals(LongStream.rangeClosed(1, last).boxed().toList(), entries, () -> "every entry once, in order: " + difference(entries));

            List<Long> pages = positions(contentStore, "durable-pages");
            for (int i = 1; i < pages.size(); i++) {
                long previous = pages.get(i - 1);
                long page = pages.get(i);
                assertTrue(page > previous, () -> "every complete page once, in order: " + previous + " then " + page);
            }
            assertEquals(1L, pages.getFirst());
            assertTrue(pages.getLast() <= last, () -> "no empty page: " + pages.getLast() + " after the last entry " + last);
        } finally {
            contentStore.close();
        }
    }

    static String difference(List<Long> positions) {
        java.util.TreeMap<Long, Integer> counts = new java.util.TreeMap<>();
        positions.forEach(position -> counts.merge(position, 1, Integer::sum));
        long last = positions.isEmpty() ? 0 : positions.getLast();
        List<Long> missing = LongStream.rangeClosed(1, last).filter(position -> !counts.containsKey(position)).boxed().toList();
        List<Long> twice = counts.entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).toList();
        List<String> backwards = new ArrayList<>();
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i) < positions.get(i - 1)) {
                backwards.add(positions.get(i - 1) + "->" + positions.get(i));
            }
        }
        return "messages " + positions.size() + ", missing " + missing + ", twice " + twice  + ", backwards " + backwards;
    }

    static List<Long> positions(ContentStore contentStore, String topic) throws InterruptedException {
        List<Long> positions = new ArrayList<>();
        ContentStreamConsumer consumer = contentStore.contentStream().consumer(topic);
        ContentStreamBuffer buffer;
        while ((buffer = consumer.receive(1, TimeUnit.SECONDS)) != null) {
            positions.add(Long.parseLong(buffer.position()));
        }
        return positions;
    }

    @Test
    void thatAResumeAfterTheLastEntryIsDurable() throws Exception {
        stopAndResume("${cast.toLong(contentStream.lastOrInitialPosition(0)) + 1}");
    }

    @Test
    void thatAResumeAtTheNextPageIsDurable() throws Exception {
        stopAndResume("${cast.toLong(contentStream.lastOrInitialPagePosition(-9)) + 10}");
    }

    @Test
    void thatAResumeAtTheLastPageIsDurable() throws Exception {
        stopAndResume("${cast.toLong(contentStream.lastOrInitialPagePosition(1))}");
    }

    @Test
    void thatAFailedPageIsNotCommitted() throws TimeoutException, InterruptedException {
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", CONFIGURATION);
        try {
            Worker worker = Worker.newBuilder()
                    // each entry buffers its content, then fails
                    .specification(specification("1", addContent("${position}", "entry"), get("entry")
                            .url(testServer.testURL("/api/does-not-exist"))
                            .validate(status().success(200, 299))))
                    .configuration(Map.of("content.stream.connector", "rawdata", "rawdata.client.provider", "memory",
                            "data.collector.http.request.napOnRetry", "1"))
                    .contentStore(contentStore)
                    .keepContentStoreOpenOnWorkerCompletion(true)
                    .build();
            assertThrows(RuntimeException.class, worker::run);
            assertTrue(positions(contentStore, "durable-pages").isEmpty());
            assertTrue(positions(contentStore, "durable").isEmpty());
            // the failed run's buffered content is gone, so a later run never publishes it (#72)
            assertEquals(0, contentStore.monitor().build().activePositionCount, "positions left buffered");
        } finally {
            contentStore.close();
        }
    }
}
