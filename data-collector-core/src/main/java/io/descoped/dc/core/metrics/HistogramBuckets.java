package io.descoped.dc.core.metrics;

/**
 * The buckets of dc's histograms.
 */
public final class HistogramBuckets {

    private HistogramBuckets() {
    }

    /**
     * The default buckets of the simpleclient histograms of 2.x, 0.005 to 10 seconds, so {@code /metrics} gives the
     * same series on client_java, whose default lacks 0.075, 0.75 and 7.5.
     *
     * @return a new array of the upper bounds
     */
    public static double[] simpleclient() {
        return new double[]{.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10};
    }
}
