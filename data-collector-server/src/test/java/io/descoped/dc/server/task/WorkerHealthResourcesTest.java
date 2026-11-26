package io.descoped.dc.server.task;

import io.descoped.dc.application.health.HealthResourceFactory;
import io.descoped.dc.core.health.HealthWorkerResource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

// #46: workers add and remove their health resources while /health lists them
class WorkerHealthResourcesTest {

    @Test
    void thatResourcesAreListedWhileWorkersAddAndRemoveThem() throws Exception {
        HealthResourceFactory factory = HealthResourceFactory.create();
        int discovered = factory.getHealthResources().size();
        AtomicBoolean done = new AtomicBoolean();

        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            for (int worker = 0; worker < 4; worker++) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    for (int i = 0; i < 2_000; i++) {
                        UUID id = UUID.randomUUID();
                        factory.createAndAdddHealthResource(id, HealthWorkerResource.class);
                        factory.removeHealthResource(id);
                    }
                }, executor));
            }
            CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
                while (!done.get()) {
                    factory.getHealthResources();
                }
            }, executor);
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[0])).get(60, TimeUnit.SECONDS);
            done.set(true);
            reader.get(60, TimeUnit.SECONDS);
        }

        assertEquals(discovered, factory.getHealthResources().size());
    }
}
