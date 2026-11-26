package io.descoped.dc.application.metrics;

import io.descoped.dc.api.metrics.MetricsResource;
import io.prometheus.metrics.core.metrics.Gauge;

import java.time.Instant;

/**
 * The server's uptime as a Prometheus metric.
 */
public class MetricsApplicationResource implements MetricsResource {

    private static final Gauge since = Gauge.builder().name("dc_application_since").help("Application started").register();
    private static final Gauge uptime = Gauge.builder().name("dc_application_uptime").help("Application uptime").register();

    /**
     * Creates the resource.
     */
    public MetricsApplicationResource() {
        since.set((double) Instant.now().toEpochMilli());
    }

    /**
     * Updates the uptime gauge: the time since the resource was created, in milliseconds.
     */
    public void trackUptime() {
        uptime.set(Instant.now().toEpochMilli() - since.get());
    }

}
