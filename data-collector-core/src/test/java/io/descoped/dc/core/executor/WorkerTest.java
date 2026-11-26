package io.descoped.dc.core.executor;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import io.descoped.dc.core.health.HealthWorkerResource;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.execute;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.paginate;
import static io.descoped.dc.api.Builders.status;
import static io.descoped.dc.api.Builders.whenVariableIsNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(TestServerExtension.class)
class WorkerTest {

    @Inject
    TestServer testServer;

    SpecificationBuilder specification(String path) {
        return Specification.start("test", "page", "page")
                .configure(context().topic("topic"))
                .function(get("page")
                        .url(testServer.testURL(path))
                        .validate(status().success(200, 299)));
    }

    @Test
    void thatBuilderAppliesHttpAndThreadConfiguration() {
        Worker worker = Worker.newBuilder()
                .specification(specification("/api/events?position=1&pageSize=1"))
                .configuration(Map.of(
                        "data.collector.http.version", "http_1_1",
                        "data.collector.http.followRedirects", "normal",
                        "data.collector.worker.threads", "2"))
                .printConfiguration()
                .printExecutionPlan()
                .build();

        Client client = worker.context().services().get(Client.class);
        assertEquals(Client.Version.HTTP_1_1, client.version());
        assertEquals("test", worker.getSpecificationId());
    }

    @Test
    void thatBuilderTakesANodeBuilderAsStart() {
        Worker worker = Worker.newBuilder()
                .specification(get("page").url(testServer.testURL("/api/events?position=1&pageSize=1")))
                .topic("topic")
                .printConfiguration()
                .build();

        assertEquals("GetBuilder", worker.getSpecificationId());
        assertEquals("GetBuilder", worker.getSpecificationName());
    }

    @Test
    void thatBuilderNeedsASpecification() {
        Worker.WorkerBuilder builder = Worker.newBuilder();
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void thatRunUpdatesTheHealthMonitor() {
        Worker worker = Worker.newBuilder()
                .specification(specification("/api/events?position=1&pageSize=1"))
                .build();
        HealthWorkerMonitor monitor = new HealthWorkerResource(worker.getWorkerId()).getMonitor();
        worker.context().services().register(HealthWorkerMonitor.class, monitor);

        ExecutionContext output = worker.run();

        assertNotNull(output);
        assertEquals(WorkerStatus.COMPLETED, monitor.status());
    }

    @Test
    void thatPaginationUpdatesTheHealthMonitor() {
        Worker worker = Worker.newBuilder()
                .specification(Specification.start("test", "loop", "loop")
                        .configure(context().topic("topic"))
                        .function(paginate("loop")
                                .iterate(execute("page"))
                                .until(whenVariableIsNull("nextPosition")))
                        .function(get("page")
                                .url(testServer.testURL("/api/events?position=1&pageSize=1"))
                                .validate(status().success(200, 299))))
                .build();
        HealthWorkerMonitor monitor = new HealthWorkerResource(worker.getWorkerId()).getMonitor();
        worker.context().services().register(HealthWorkerMonitor.class, monitor);

        worker.run();

        assertEquals(WorkerStatus.COMPLETED, monitor.status());
    }

    @Test
    void thatRunAsyncPassesTheFailureOn() {
        Worker worker = Worker.newBuilder()
                .specification(specification("/api/does-not-exist"))
                .build();
        HealthWorkerMonitor monitor = new HealthWorkerResource(UUID.randomUUID()).getMonitor();
        worker.context().services().register(HealthWorkerMonitor.class, monitor);

        CompletableFuture<ExecutionContext> future = worker.runAsync();

        assertThrows(CompletionException.class, future::join);
        assertEquals(WorkerStatus.FAILED, monitor.status());
    }

    // #46: the contexts of a worker share its global state on every thread of its pool
    @Test
    void thatTheGlobalStateIsSharedSafely() throws Exception {
        Worker worker = Worker.newBuilder()
                .specification(specification("/api/events"))
                .build();
        ExecutionContext context = worker.context();
        int threads = 8;
        int keys = 500;

        List<CompletableFuture<Void>> writers = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (int thread = 0; thread < threads; thread++) {
                int t = thread;
                writers.add(CompletableFuture.runAsync(() -> {
                    for (int key = 0; key < keys; key++) {
                        ExecutionContext.of(context).globalState("key-" + t + "-" + key, key);
                        ExecutionContext.of(context).join(ExecutionContext.of(context));
                        // a context of its own global state, as a handler joins the specification's global context
                        ExecutionContext.of(context).join(ExecutionContext.empty());
                    }
                }, executor));
            }
            CompletableFuture.allOf(writers.toArray(new CompletableFuture<?>[0])).get(60, TimeUnit.SECONDS);
        }

        for (int thread = 0; thread < threads; thread++) {
            for (int key = 0; key < keys; key += 50) {
                assertEquals(key, (Integer) context.state("key-" + thread + "-" + key), "key-" + thread + "-" + key);
            }
        }
    }
}
