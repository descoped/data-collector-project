package io.descoped.dc.core;

import io.descoped.dc.core.executor.FixedThreadPool;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletableTest {

    final Supplier<String> currentThreadName = () -> Thread.currentThread().getName();

    @Test
    void testCompletables() throws InterruptedException {
        FixedThreadPool threadPool = FixedThreadPool.newInstance();

        Supplier<String> nestedFuture = currentThreadName;

        CompletableFuture<String> nestedFutureWithExecutor = CompletableFuture.supplyAsync(() -> {
                    String msg = currentThreadName.get();
                    System.out.printf("HERE 1: %s%n", Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return msg;
                }, threadPool.getExecutor())
                .thenApply(s -> {
                    System.out.printf("HERE 2: %s%n", Thread.currentThread().getName());
                    return s;
                });

        Thread.sleep(1500);

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    String msg = currentThreadName.get();
                    System.out.printf("supplyAsync: origin: %s -- nested: %s%n", msg, nestedFuture.get());
                    return msg;

                }, threadPool.getExecutor()).thenApply(msg -> {
                    System.out.printf("apply: origin: %s -- this: %s -- nested: %s%n", msg, currentThreadName.get(), nestedFuture.get());
                    nestedFuture.get();
                    return msg;

                }).thenApply(msg -> {
                    System.out.printf("apply: origin: %s -- this: %s -- nested: %s%n", msg, currentThreadName.get(), nestedFuture.get());
                    nestedFuture.get();
                    return msg;
                }).thenRunAsync(() -> {
                    String msg = currentThreadName.get();
                    System.out.printf("runAsyncWithExector: origin: %s nested: %s%n", msg, nestedFutureWithExecutor.join());
                }, threadPool.getExecutor())
                .thenRunAsync(() -> {
                    String msg = currentThreadName.get();
                    System.out.printf("runAsync: origin: %s nested: %s%n", msg, nestedFutureWithExecutor.join());
                });

        future.join();
        assertTrue(future.isDone());
        assertNotNull(nestedFutureWithExecutor.join());
    }
}
