package io.descoped.dc.core.handler;

import io.descoped.dc.api.Specification;
import io.descoped.dc.core.executor.Worker;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.get;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationHandlerTest {

    // nothing listens on port 1 of the loopback address, so the connection is refused on each try
    @Test
    void thatRefusedConnectionFailsAfterTheConfiguredRetries() {
        Worker worker = Worker.newBuilder()
                .specification(Specification.start("test", "page", "page")
                        .configure(context().topic("topic"))
                        .function(get("page").url("http://127.0.0.1:1/")))
                .configuration(Map.of(
                        "data.collector.http.request.retryCount", "1",
                        // read from retryCount (see the PR's "Found"), so the nap is 1 ms
                        "data.collector.http.request.napOnRetryInMillis", "10"))
                .build();

        assertThrows(RuntimeException.class, worker::run);
    }
}
