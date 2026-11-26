package io.descoped.dc.core.content;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
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

    private static final String SUBSYSTEM = "content_stream";
    private static final String TOPIC = "topic";

    private ContentStoreExporter() {
    }

    static final Counter writePaginationPageContent = Counter.build("pagination_page_publish_count", "Number of written pagination pages")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
            .register();

    static final Histogram writePaginationPageContentDuration = Histogram.build("pagination_page_publish_duration", "Pagination page write duration")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
            .register();

    static final Counter bufferPaginationPageEntryContent = Counter.build("buffer_pagination_entry_count", "Buffer pagination entry count")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
            .register();

    static final Counter bufferPageEntryDocumentContent = Counter.build("buffer_entry_document_count", "Buffer entry document count")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
            .register();

    static final Counter publishPositionContent = Counter.build("pagination_entry_publish_position_count", "Number of published positions")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
            .register();

    static final Histogram publishPositionContentDuration = Histogram.build("pagination_entry_publish_position_duration", "Published position write duration")
            .namespace("dc")
            .subsystem(SUBSYSTEM)
            .labelNames(TOPIC)
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
        return writePaginationPageContentDuration.labels(topic).startTimer();
    }

    /**
     * Ends timing a pagination page and counts it.
     *
     * @param timer the timer from {@link #paginationStarted}
     * @param topic the topic
     */
    public static void paginationCompleted(Object timer, String topic) {
        observe(timer);
        writePaginationPageContent.labels(topic).inc();
    }

    /**
     * Counts a buffered pagination entry.
     *
     * @param topic  the topic
     * @param thrown what buffering threw, or {@code null}
     */
    public static void entryBuffered(String topic, Throwable thrown) {
        if (thrown == null) {
            bufferPaginationPageEntryContent.labels(topic).inc();
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
            bufferPageEntryDocumentContent.labels(topic).inc();
        }
    }

    /**
     * Starts timing a publication.
     *
     * @param topic the topic
     * @return the timer
     */
    public static Object publishStarted(String topic) {
        return publishPositionContentDuration.labels(topic).startTimer();
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
        publishPositionContent.labels(topic).inc(positions.length);
    }

    private static void observe(Object timer) {
        if (timer instanceof Histogram.Timer histogramTimer) {
            histogramTimer.observeDuration();
        }
    }
}
