package io.descoped.dc.core.metrics;

import io.descoped.dc.api.observer.ContentEvent;
import io.descoped.dc.api.observer.EngineObserver;
import io.descoped.dc.api.observer.NodeEvent;
import io.descoped.dc.api.observer.Observation;
import io.descoped.dc.api.observer.RequestEvent;
import io.descoped.dc.api.observer.RequestObservation;
import io.descoped.dc.api.observer.WorkerEvent;
import io.prometheus.metrics.core.datapoints.Timer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;

import java.util.Objects;

/**
 * The engine's Prometheus metrics, in the default registry: the client and content store metrics of 2.x, under the
 * same names, labels and buckets, and the metrics of workers, nodes, retries, pages and fan-out.
 */
public class PrometheusEngineObserver implements EngineObserver {

    private static final String LOCATION = "location";
    private static final String TOPIC = "topic";

    static final Counter requestStartedCount = Counter.builder()
            .name("dc_client_request_started")
            .help("Started requests")
            .labelNames(LOCATION)
            .register();

    static final Counter requestCompletedCount = Counter.builder()
            .name("dc_client_request_completed")
            .help("Completed requests")
            .labelNames(LOCATION, "statusCode", "emptyResponse")
            .register();

    static final Counter requestFailureCount = Counter.builder()
            .name("dc_client_request_failure")
            .help("Failed requests")
            .labelNames(LOCATION)
            .register();

    static final Histogram requestDurationHistogram = Histogram.builder()
            .name("dc_client_request_duration")
            .help("Request duration")
            .labelNames(LOCATION)
            .classicOnly()
            .classicUpperBounds(HistogramBuckets.simpleclient())
            .register();

    static final Counter requestRetryCount = Counter.builder()
            .name("dc_client_request_retry")
            .help("Requests tried again after a failure")
            .labelNames(LOCATION)
            .register();

    static final Counter writePaginationPageContent = Counter.builder()
            .name("dc_content_stream_pagination_page_publish_count")
            .help("Number of written pagination pages")
            .labelNames(TOPIC)
            .register();

    static final Histogram writePaginationPageContentDuration = Histogram.builder()
            .name("dc_content_stream_pagination_page_publish_duration")
            .help("Pagination page write duration")
            .labelNames(TOPIC)
            .classicOnly()
            .classicUpperBounds(HistogramBuckets.simpleclient())
            .register();

    static final Counter bufferPaginationPageEntryContent = Counter.builder()
            .name("dc_content_stream_buffer_pagination_entry_count")
            .help("Buffer pagination entry count")
            .labelNames(TOPIC)
            .register();

    static final Counter bufferPageEntryDocumentContent = Counter.builder()
            .name("dc_content_stream_buffer_entry_document_count")
            .help("Buffer entry document count")
            .labelNames(TOPIC)
            .register();

    static final Counter publishPositionContent = Counter.builder()
            .name("dc_content_stream_pagination_entry_publish_position_count")
            .help("Number of published positions")
            .labelNames(TOPIC)
            .register();

    static final Histogram publishPositionContentDuration = Histogram.builder()
            .name("dc_content_stream_pagination_entry_publish_position_duration")
            .help("Published position write duration")
            .labelNames(TOPIC)
            .classicOnly()
            .classicUpperBounds(HistogramBuckets.simpleclient())
            .register();

    static final Counter workerStartedCount = Counter.builder()
            .name("dc_worker_started")
            .help("Started workers")
            .labelNames("specification")
            .register();

    static final Counter workerFinishedCount = Counter.builder()
            .name("dc_worker_finished")
            .help("Finished workers, by how they ended")
            .labelNames("specification", "status")
            .register();

    static final Histogram nodeDurationHistogram = Histogram.builder()
            .name("dc_node_duration")
            .help("Node execution duration")
            .labelNames("type")
            .classicOnly()
            .classicUpperBounds(HistogramBuckets.simpleclient())
            .register();

    static final Counter nodeFailureCount = Counter.builder()
            .name("dc_node_failure")
            .help("Node executions that threw")
            .labelNames("type")
            .register();

    static final Counter paginationPageCount = Counter.builder()
            .name("dc_pagination_page")
            .help("Pages completed by paginate nodes")
            .labelNames("node")
            .register();

    static final Counter parallelEntryCount = Counter.builder()
            .name("dc_parallel_entry")
            .help("Entries fanned out by parallel nodes")
            .register();

    /**
     * Creates the observer; the engine finds it with {@link java.util.ServiceLoader}.
     */
    public PrometheusEngineObserver() {
        // the metrics are registered once, with the class
    }

    @Override
    public void workerStarted(WorkerEvent worker) {
        workerStartedCount.labelValues(label(worker.specificationId())).inc();
    }

    @Override
    public void workerFinished(WorkerEvent worker, String status) {
        workerFinishedCount.labelValues(label(worker.specificationId()), label(status)).inc();
    }

    @Override
    public Observation nodeStarted(NodeEvent node) {
        String type = label(node.type());
        Timer timer = nodeDurationHistogram.labelValues(type).startTimer();
        return new Observation() {
            @Override
            public void failed(Throwable failure) {
                nodeFailureCount.labelValues(type).inc();
            }

            @Override
            public void close() {
                timer.observeDuration();
            }
        };
    }

    @Override
    public RequestObservation requestStarted(RequestEvent request) {
        String location = label(request.location());
        Timer timer = requestDurationHistogram.labelValues(location).startTimer();
        requestStartedCount.labelValues(location).inc();
        return new RequestObservation() {
            @Override
            public void responded(int statusCode, boolean emptyResponse) {
                requestCompletedCount.labelValues(location, String.valueOf(statusCode), String.valueOf(emptyResponse)).inc();
            }

            @Override
            public void failed(Throwable failure) {
                requestFailureCount.labelValues(location).inc();
            }

            @Override
            public void close() {
                timer.observeDuration();
            }
        };
    }

    @Override
    public void requestRetried(RequestEvent request, int attempt) {
        requestRetryCount.labelValues(label(request.location())).inc();
    }

    @Override
    public void operationFailed(RequestEvent request, Throwable failure) {
        requestFailureCount.labelValues(label(request.location())).inc();
    }

    @Override
    public void pageCompleted(NodeEvent paginate) {
        paginationPageCount.labelValues(label(paginate.id())).inc();
    }

    @Override
    public void entryFannedOut(NodeEvent parallel) {
        parallelEntryCount.inc();
    }

    @Override
    public Observation addingPage(ContentEvent content) {
        String topic = label(content.topic());
        Timer timer = writePaginationPageContentDuration.labelValues(topic).startTimer();
        // counted whether or not the page is written, as in 2.x
        return new Observation() {
            @Override
            public void close() {
                timer.observeDuration();
                writePaginationPageContent.labelValues(topic).inc();
            }
        };
    }

    @Override
    public void entryBuffered(ContentEvent content) {
        bufferPaginationPageEntryContent.labelValues(label(content.topic())).inc();
    }

    @Override
    public void documentBuffered(ContentEvent content) {
        bufferPageEntryDocumentContent.labelValues(label(content.topic())).inc();
    }

    @Override
    public Observation publishing(ContentEvent content, int positions) {
        String topic = label(content.topic());
        Timer timer = publishPositionContentDuration.labelValues(topic).startTimer();
        // counted whether or not the positions are published, as in 2.x
        return new Observation() {
            @Override
            public void close() {
                timer.observeDuration();
                publishPositionContent.labelValues(topic).inc(positions);
            }
        };
    }

    // prometheus: a label is never null
    private static String label(String value) {
        return Objects.toString(value, "");
    }
}
