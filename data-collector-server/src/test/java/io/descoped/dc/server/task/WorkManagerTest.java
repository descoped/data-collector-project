package io.descoped.dc.server.task;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.TerminationException;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

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
import static io.descoped.dc.api.Builders.whenVariableIsNull;
import static io.descoped.dc.api.Builders.xpath;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestServerExtension.class)
class WorkManagerTest {

    @Inject
    TestServer testServer;

    // pages through the mock feed to stopAt; -1 never stops
    Worker.WorkerBuilder worker(String specificationId, int stopAt) {
        SpecificationBuilder specification = Specification.start(specificationId, "pages", "loop")
                .configure(context()
                        .topic(specificationId)
                        .header("accept", "application/xml")
                        .variable("nextPosition", "1"))
                .function(paginate("loop")
                        .variable("fromPosition", "${nextPosition}")
                        .iterate(execute("page"))
                        .prefetchThreshold(5)
                        .until(whenVariableIsNull("nextPosition")))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=${fromPosition}&pageSize=10&stopAt=" + stopAt))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(nextPage()
                                .output("nextPosition", regex(xpath("/feed/link[@rel=\"next\"]/@href"), "(?<=[?&]position=)[^&]*")))
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(addContent("${position}", "entry"))
                                .pipe(publish("${position}")))
                        .returnVariables("nextPosition"));
        return Worker.newBuilder()
                .specification(specification)
                .configuration(Map.of("content.stream.connector", "rawdata", "rawdata.client.provider", "memory"));
    }

    // #45: the future of a worker that succeeds completes with its output, not with a NullPointerException
    @Test
    void thatASuccessfulWorkersFutureCompletesNormally() throws Exception {
        WorkManager workManager = new WorkManager();
        WorkManager.JobId jobId = workManager.run(worker("work-manager-success", 20));

        ExecutionContext output = workManager.future(jobId).get(30, TimeUnit.SECONDS);

        assertNotNull(output);
    }

    // #45: cancel stops the running workers and waits for them
    @Test
    void thatCancelStopsTheRunningWorkers() {
        WorkManager workManager = new WorkManager();
        WorkManager.JobId jobId = workManager.run(worker("work-manager-cancel", -1));
        CompletableFuture<ExecutionContext> future = workManager.future(jobId);
        assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.SECONDS), "the worker pages without end");

        workManager.cancel(Duration.ofSeconds(30));

        assertTrue(future.isDone(), "the worker stopped within the timeout");
        ExecutionException stopped = assertThrows(ExecutionException.class, future::get);
        assertTrue(causes(stopped).anyMatch(TerminationException.class::isInstance), "it ended with its termination: " + stopped);
    }

    static Stream<Throwable> causes(Throwable throwable) {
        return Stream.iterate(throwable, Objects::nonNull, Throwable::getCause);
    }
}
