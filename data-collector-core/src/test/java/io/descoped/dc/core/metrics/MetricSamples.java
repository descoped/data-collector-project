package io.descoped.dc.core.metrics;

import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;

/**
 * Reads samples of the default Prometheus registry in tests, as simpleclient's {@code getSampleValue} did.
 */
public final class MetricSamples {

    private MetricSamples() {
    }

    /**
     * The value of a sample, named as {@code /metrics} shows it: a counter with {@code _total}, a histogram's count
     * with {@code _count}.
     *
     * @param sampleName  the sample's name
     * @param labelNames  the label names
     * @param labelValues the label values, in the same order
     * @return the value, or {@code 0} when there is no such sample
     */
    public static double value(String sampleName, String[] labelNames, String[] labelValues) {
        String metricName = sampleName.replaceFirst("_(total|count)$", "");
        Labels labels = Labels.of(labelNames, labelValues);
        for (MetricSnapshot snapshot : PrometheusRegistry.defaultRegistry.scrape(metricName::equals)) {
            for (DataPointSnapshot dataPoint : snapshot.getDataPoints()) {
                if (!dataPoint.getLabels().equals(labels)) {
                    continue;
                }
                if (dataPoint instanceof CounterSnapshot.CounterDataPointSnapshot counter) {
                    return counter.getValue();
                }
                if (dataPoint instanceof GaugeSnapshot.GaugeDataPointSnapshot gauge) {
                    return gauge.getValue();
                }
                if (dataPoint instanceof HistogramSnapshot.HistogramDataPointSnapshot histogram) {
                    return histogram.getCount();
                }
            }
        }
        return 0;
    }

    /**
     * The registry in the Prometheus text format.
     *
     * @return the text
     */
    public static String text() {
        return PrometheusTextFormatWriter.create().toDebugString(PrometheusRegistry.defaultRegistry.scrape());
    }
}
