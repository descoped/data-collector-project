package io.descoped.dc.core.handler;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.Map;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.execute;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.paginate;
import static io.descoped.dc.api.Builders.nextPage;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.regex;
import static io.descoped.dc.api.Builders.sequence;
import static io.descoped.dc.api.Builders.status;
import static io.descoped.dc.api.Builders.whenVariableIsNull;
import static io.descoped.dc.api.Builders.xpath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
class ParallelHandlerTest {

    @Inject
    TestServer testServer;

    SpecificationBuilder specification(String entryUrl) {
        return Specification.start("test", "page", entryUrl == null ? "page" : "loop")
                .configure(context().topic("topic"))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=1&pageSize=5"))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(entryUrl == null ? addContent("${position}", "entry") : get("entry")
                                        .url(testServer.testURL(entryUrl))
                                        .validate(status().success(200, 299)))
                                .pipe(publish("${position}"))));
    }

    @Test
    void thatBufferedFanOutWaitsAtEachThreshold() {
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of());

        Worker.newBuilder()
                .specification(specification(null))
                .configuration(Map.of("data.collector.parallel.queueBuffer.threshold", "2"))
                .contentStore(contentStore)
                .header("Accept", "application/xml")
                .build()
                .run();

        assertEquals("5", contentStore.lastPosition("topic"));
    }

    // the pagination lifecycle joins the page's parallel futures, so a failing entry fails the worker
    @Test
    void thatAFailingEntryFailsThePagination() {
        Worker worker = Worker.newBuilder()
                .specification(specification("/api/does-not-exist")
                        .function(paginate("loop")
                                .iterate(execute("page"))
                                .until(whenVariableIsNull("nextPosition"))))
                .header("Accept", "application/xml")
                .build();

        assertThrows(RuntimeException.class, worker::run);
    }

    // #24: outside a paginate, the worker waits for the entries it started, so a failing entry fails it too
    @Test
    void thatAFailingEntryFailsTheWorker() {
        SpecificationBuilder specification = Specification.start("test", "page", "page")
                .configure(context().topic("topic"))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=1&pageSize=5"))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(get("entry")
                                        .url(testServer.testURL("/api/does-not-exist"))
                                        .validate(status().success(200, 299)))));
        Worker worker = Worker.newBuilder()
                .specification(specification)
                .configuration(Map.of("data.collector.http.request.napOnRetry", "1"))
                .header("Accept", "application/xml")
                .build();

        assertThrows(RuntimeException.class, worker::run);
    }

    // each worker counts its own entries against its own limit (#66)
    @Test
    void thatEachWorkerCountsItsOwnIterations() {
        String firstRun = runWithIterationLimit();
        // a second worker, after the first has counted past the limit, ends at the same position
        String secondRun = runWithIterationLimit();
        assertNotNull(firstRun);
        assertNotEquals("100", firstRun);
        assertEquals(firstRun, secondRun);
    }

    private String runWithIterationLimit() {
        SpecificationBuilder specification = Specification.start("test", "pages", "loop")
                .configure(context()
                        .topic("topic")
                        .header("accept", "application/xml")
                        .variable("nextPosition", "1"))
                .function(paginate("loop")
                        .variable("fromPosition", "${nextPosition}")
                        .iterate(execute("page"))
                        .prefetchThreshold(5)
                        .until(whenVariableIsNull("nextPosition")))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=${fromPosition}&pageSize=5&stopAt=100"))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(nextPage()
                                .output("nextPosition", regex(xpath("/feed/link[@rel=\"next\"]/@href"), "(?<=[?&]position=)[^&]*")))
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(addContent("${position}", "entry"))
                                .pipe(publish("${position}")))
                        .returnVariables("nextPosition"));
        ContentStore contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, "discarding", Map.of());
        Worker.newBuilder()
                .specification(specification)
                .contentStore(contentStore)
                .stopAtNumberOfIterations(12)
                .build()
                .run();
        return contentStore.lastPosition("topic");
    }
}
