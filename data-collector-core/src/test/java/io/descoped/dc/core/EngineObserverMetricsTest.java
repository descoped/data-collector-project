package io.descoped.dc.core;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.core.http.URLInfo;
import io.descoped.dc.core.metrics.MetricSamples;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import java.util.Map;

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
 * A worker run is reported to the engine observers, and the Prometheus observer counts workers, nodes, pages,
 * fanned-out entries, retries and failed GET steps.
 */
@ExtendWith(TestServerExtension.class)
class EngineObserverMetricsTest {

    @Inject
    TestServer testServer;

    static double sample(String name, String labelName, String labelValue) {
        return MetricSamples.value(name, new String[]{labelName}, new String[]{labelValue});
    }

    static double sample(String name) {
        return MetricSamples.value(name, new String[0], new String[0]);
    }

    static double finished(String specification, String status) {
        return MetricSamples.value("dc_worker_finished_total", new String[]{"specification", "status"}, new String[]{specification, status});
    }

    @Test
    void thatAPaginatedRunIsCounted() {
        String specification = "observer-paginate";
        double started = sample("dc_worker_started_total", "specification", specification);
        double completed = finished(specification, "COMPLETED");
        double pages = sample("dc_pagination_page_total", "node", "page-loop");
        double entries = sample("dc_parallel_entry_total");
        double gets = sample("dc_node_duration_count", "type", "Get");

        SpecificationBuilder specificationBuilder = Specification.start(specification, "paginate", "page-loop")
                .configure(context()
                        .topic("observer-topic")
                        .header("accept", "application/xml")
                        .variable("baseURL", testServer.testURL(""))
                        .variable("nextPosition", "1")
                )
                .function(paginate("page-loop")
                        .variable("fromPosition", "${nextPosition}")
                        .addPageContent("fromPosition")
                        .iterate(execute("page"))
                        .prefetchThreshold(5)
                        .until(whenVariableIsNull("nextPosition"))
                )
                .function(get("page")
                        .url("${baseURL}/api/events?position=${fromPosition}&pageSize=10&stopAt=30")
                        .validate(status().success(200, 299).fail(300, 599))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id"))
                        )
                        .pipe(nextPage()
                                .output("nextPosition", regex(xpath("/feed/link[@rel=\"next\"]/@href"), "(?<=[?&]position=)[^&]*"))
                        )
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(addContent("${position}", "entry"))
                                .pipe(publish("${position}"))
                        )
                        .returnVariables("nextPosition")
                );

        Worker.newBuilder()
                .specification(specificationBuilder)
                .configuration(Map.of("content.stream.connector", "discarding"))
                .build()
                .run();

        assertEquals(started + 1, sample("dc_worker_started_total", "specification", specification));
        assertEquals(completed + 1, finished(specification, "COMPLETED"));
        assertTrue(sample("dc_pagination_page_total", "node", "page-loop") >= pages + 3, MetricSamples.text());
        assertTrue(sample("dc_parallel_entry_total") >= entries + 30, MetricSamples.text());
        assertTrue(sample("dc_node_duration_count", "type", "Get") >= gets + 3, MetricSamples.text());
    }

    @Test
    void thatAFailedGetIsCounted() {
        String specification = "observer-failure";
        String url = testServer.testURL("/api/does-not-exist");
        String location = new URLInfo(url).getLocation().orElse("");
        double failed = finished(specification, "FAILED");
        double requestFailures = sample("dc_client_request_failure_total", "location", location);
        double getFailures = sample("dc_node_failure_total", "type", "Get");
        double retries = sample("dc_client_request_retry_total", "location", location);

        SpecificationBuilder specificationBuilder = Specification.start(specification, "failure", "page")
                .function(get("page")
                        .url(url)
                        .validate(status().success(200))
                );

        Worker worker = Worker.newBuilder()
                .specification(specificationBuilder)
                .configuration(Map.of("content.stream.connector", "discarding",
                        "data.collector.http.request.retryCount", "3",
                        "data.collector.http.request.napOnRetry", "1"))
                .build();
        assertThrows(RuntimeException.class, worker::run);

        assertEquals(failed + 1, finished(specification, "FAILED"));
        // three attempts fail: two of them are tried again
        assertEquals(retries + 2, sample("dc_client_request_retry_total", "location", location), MetricSamples.text());
        assertTrue(sample("dc_client_request_failure_total", "location", location) >= requestFailures + 1, MetricSamples.text());
        assertTrue(sample("dc_node_failure_total", "type", "Get") >= getFailures + 1, MetricSamples.text());
    }
}
