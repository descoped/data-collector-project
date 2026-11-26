package io.descoped.dc.core.content;

import io.prometheus.metrics.core.datapoints.Timer;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Histogram;
import net.bytebuddy.asm.Advice;


/**
 * Metrics:
 * <p>
 * pagination_page_publish_count
 * pagination_page_public_duration
 * buffer_pagination_entry_count
 * buffer_entry_document_count
 * pagination_entry_publish_position_count
 * pagination_entry_publish_position_duration
 * <p>
 * The interceptors are Advice that {@link ContentStoreAgent} inlines into the content stores, so they call only public
 * methods here.
 */
public class ContentStoreExporter {

    // the buckets of the simpleclient histograms of 2.x, so /metrics gives the same series on client_java
    private static final double[] SIMPLECLIENT_BUCKETS = {.005, .01, .025, .05, .075, .1, .25, .5, .75, 1, 2.5, 5, 7.5, 10};
    private static final String SUBSYSTEM = "content_stream";
    private static final String TOPIC = "topic";

    private ContentStoreExporter() {
    }

    static final Counter writePaginationPageContent = Counter.builder()
            .name("dc_" + SUBSYSTEM + "_pagination_page_publish_count")
            .help("Number of written pagination pages")
            .labelNames(TOPIC)
            .register();

    static final Histogram writePaginationPageContentDuration = Histogram.builder()
            .name("dc_" + SUBSYSTEM + "_pagination_page_publish_duration")
            .help("Pagination page write duration")
            .labelNames(TOPIC)
            .classicUpperBounds(SIMPLECLIENT_BUCKETS)
            .register();

    static final Counter bufferPaginationPageEntryContent = Counter.builder()
            .name("dc_" + SUBSYSTEM + "_buffer_pagination_entry_count")
            .help("Buffer pagination entry count")
            .labelNames(TOPIC)
            .register();

    static final Counter bufferPageEntryDocumentContent = Counter.builder()
            .name("dc_" + SUBSYSTEM + "_buffer_entry_document_count")
            .help("Buffer entry document count")
            .labelNames(TOPIC)
            .register();

    static final Counter publishPositionContent = Counter.builder()
            .name("dc_" + SUBSYSTEM + "_pagination_entry_publish_position_count")
            .help("Number of published positions")
            .labelNames(TOPIC)
            .register();

    static final Histogram publishPositionContentDuration = Histogram.builder()
            .name("dc_" + SUBSYSTEM + "_pagination_entry_publish_position_duration")
            .help("Published position write duration")
            .labelNames(TOPIC)
            .classicUpperBounds(SIMPLECLIENT_BUCKETS)
            .register();

    /**
     * Advice for {@code ContentStore.addPaginationDocument}.
     */
    public static final class Paginate {

        private Paginate() {
        }

        /**
         * Starts timing.
         *
         * @param topic the topic
         * @return the timer
         */
        @Advice.OnMethodEnter
        public static Object enter(@Advice.Argument(0) String topic) {
            return paginationStarted(topic);
        }

        /**
         * Ends timing and counts the page.
         *
         * @param timer the timer
         * @param topic the topic
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter Object timer, @Advice.Argument(0) String topic) {
            paginationCompleted(timer, topic);
        }
    }

    /**
     * Advice for {@code ContentStore.bufferPaginationEntryDocument}.
     */
    public static final class Entry {

        private Entry() {
        }

        /**
         * Counts the entry when it was buffered.
         *
         * @param topic  the topic
         * @param thrown what was thrown
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Argument(0) String topic, @Advice.Thrown Throwable thrown) {
            entryBuffered(topic, thrown);
        }
    }

    /**
     * Advice for {@code ContentStore.bufferDocument}.
     */
    public static final class Document {

        private Document() {
        }

        /**
         * Counts the document when it was buffered.
         *
         * @param topic  the topic
         * @param thrown what was thrown
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Argument(0) String topic, @Advice.Thrown Throwable thrown) {
            documentBuffered(topic, thrown);
        }
    }

    /**
     * Advice for {@code ContentStore.publish}.
     */
    public static final class Publish {

        private Publish() {
        }

        /**
         * Starts timing.
         *
         * @param topic the topic
         * @return the timer
         */
        @Advice.OnMethodEnter
        public static Object enter(@Advice.Argument(0) String topic) {
            return publishStarted(topic);
        }

        /**
         * Ends timing and counts the positions.
         *
         * @param timer     the timer
         * @param topic     the topic
         * @param positions the positions
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.Enter Object timer, @Advice.Argument(0) String topic, @Advice.Argument(1) String[] positions) {
            publishCompleted(timer, topic, positions);
        }
    }

    // the advice is inlined into stores in other modules, so it reaches the metrics through these public methods

    /**
     * Starts timing a pagination page.
     *
     * @param topic the topic
     * @return the timer
     */
    public static Object paginationStarted(String topic) {
        return writePaginationPageContentDuration.labelValues(topic).startTimer();
    }

    /**
     * Ends timing a pagination page and counts it.
     *
     * @param timer the timer from {@link #paginationStarted}
     * @param topic the topic
     */
    public static void paginationCompleted(Object timer, String topic) {
        observe(timer);
        writePaginationPageContent.labelValues(topic).inc();
    }

    /**
     * Counts a buffered pagination entry.
     *
     * @param topic  the topic
     * @param thrown what buffering threw, or {@code null}
     */
    public static void entryBuffered(String topic, Throwable thrown) {
        if (thrown == null) {
            bufferPaginationPageEntryContent.labelValues(topic).inc();
        }
    }

    /**
     * Counts a buffered document.
     *
     * @param topic  the topic
     * @param thrown what buffering threw, or {@code null}
     */
    public static void documentBuffered(String topic, Throwable thrown) {
        if (thrown == null) {
            bufferPageEntryDocumentContent.labelValues(topic).inc();
        }
    }

    /**
     * Starts timing a publication.
     *
     * @param topic the topic
     * @return the timer
     */
    public static Object publishStarted(String topic) {
        return publishPositionContentDuration.labelValues(topic).startTimer();
    }

    /**
     * Ends timing a publication and counts its positions.
     *
     * @param timer     the timer from {@link #publishStarted}
     * @param topic     the topic
     * @param positions the positions
     */
    public static void publishCompleted(Object timer, String topic, String[] positions) {
        observe(timer);
        publishPositionContent.labelValues(topic).inc(positions.length);
    }

    private static void observe(Object timer) {
        if (timer instanceof Timer histogramTimer) {
            histogramTimer.observeDuration();
        }
    }
}
