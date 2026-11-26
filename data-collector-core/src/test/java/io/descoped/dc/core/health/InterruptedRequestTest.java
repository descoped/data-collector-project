package io.descoped.dc.core.health;

import io.descoped.dc.api.Specification;
import io.descoped.dc.core.executor.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;

import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * In the health package to read the monitor's retry count. OperationHandler stops retrying when the thread is
 * interrupted (the worker is being stopped), instead of running through the remaining tries at once.
 */
class InterruptedRequestTest {

    // a failed assertion must not leave the flag set for the next test
    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    static boolean thrownFromTheRetryLoop(Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (Arrays.stream(t.getStackTrace()).anyMatch(frame -> frame.getMethodName().equals("sendAndRetryRequestOnError"))) {
                return true;
            }
        }
        return false;
    }

    @Test
    void thatAnInterruptedRequestIsNotRetried() throws IOException {
        // accepts the connection and never answers
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Worker worker = Worker.newBuilder()
                    .specification(Specification.start("test", "page", "page")
                            .configure(context().topic("topic"))
                            .function(get("page").url("http://127.0.0.1:" + server.getLocalPort() + "/")))
                    .configuration(Map.of("data.collector.http.request.retryCount", "3"))
                    .build();
            HealthWorkerMonitor monitor = new HealthWorkerResource(worker.getWorkerId()).getMonitor();
            worker.context().services().register(HealthWorkerMonitor.class, monitor);

            Thread.currentThread().interrupt();
            RuntimeException e = assertThrows(RuntimeException.class, worker::run);

            assertTrue(thrownFromTheRetryLoop(e), () -> "not thrown from the retry loop: " + e);
            assertEquals(0, monitor.request().requestRetryOnFailureCountRef.get());
        }
    }
}
