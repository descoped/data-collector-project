package io.descoped.dc.application.metrics;

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsResourcesTest {

    @Test
    void testMetricsFactory() throws InterruptedException {
        MetricsResourceFactory factory = MetricsResourceFactory.create();
        MetricsApplicationResource applicationResource = factory.get(MetricsApplicationResource.class);
        Thread.sleep(250);
        applicationResource.trackUptime();
        String text = PrometheusTextFormatWriter.create().toDebugString(PrometheusRegistry.defaultRegistry.scrape());
        System.out.printf("%s%n", text);
        assertTrue(text.contains("dc_application_uptime"));
    }
}
