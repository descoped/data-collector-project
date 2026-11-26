package io.descoped.dc.api.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthResourceUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * The counters of a content store that the health endpoint shows: documents written, buffered and published, with
 * their sizes and write durations. The counters are thread safe.
 */
public class HealthContentStreamMonitor {

    final Supplier<Boolean> contentStreamClosedCallback;
    private final Supplier<Integer> activePositionCountSupplier;
    final Supplier<Integer> activeBufferCountSupplier;

    final AtomicLong lastSeenTimestampRef = new AtomicLong(0);

    final AtomicLong paginationDocumentCountRef = new AtomicLong(0);
    final AtomicLong paginationDocumentSizeRef = new AtomicLong(0);
    final AtomicLong lastPaginationDocumentWriteDurationRef = new AtomicLong(0);
    final AtomicLong aggregatedPaginationDocumentWriteDurationRef = new AtomicLong(0);

    final AtomicLong entryBufferCountRef = new AtomicLong(0);
    final AtomicLong entryBufferSizeRef = new AtomicLong(0);
    final AtomicLong lastEntryBufferWriteDurationRef = new AtomicLong(0);
    final AtomicLong aggregatedEntryBufferWriteDurationRef = new AtomicLong(0);

    final AtomicLong documentBufferCountRef = new AtomicLong(0);
    final AtomicLong documentBufferSizeRef = new AtomicLong(0);
    final AtomicLong lastDocumentBufferWriteDurationRef = new AtomicLong(0);
    final AtomicLong aggregatedDocumentBufferWriteDurationRef = new AtomicLong(0);

    final AtomicLong publishedBufferCountRef = new AtomicLong(0);
    final AtomicLong lastPublishedBufferCountRef = new AtomicLong(0);
    final AtomicLong publishedPositionCountRef = new AtomicLong(0);
    final AtomicLong publishedCountRef = new AtomicLong(0);
    final AtomicLong lastPublishWriteDurationRef = new AtomicLong(0);
    final AtomicLong aggregatedPublishWriteDurationRef = new AtomicLong(0);

    /**
     * Creates the monitor.
     *
     * @param contentStreamClosedCallback whether the content stream is closed
     * @param activePositionCountSupplier the number of positions buffered
     * @param activeBufferCountSupplier   the number of content buffers buffered
     */
    public HealthContentStreamMonitor(Supplier<Boolean> contentStreamClosedCallback, Supplier<Integer> activePositionCountSupplier, Supplier<Integer> activeBufferCountSupplier) {
        this.contentStreamClosedCallback = contentStreamClosedCallback;
        this.activePositionCountSupplier = activePositionCountSupplier;
        this.activeBufferCountSupplier = activeBufferCountSupplier;
    }

    /**
     * Whether the content stream is open.
     *
     * @return {@code true} if it is not closed
     */
    public boolean isUp() {
        return !contentStreamClosedCallback.get();
    }

    /**
     * Sets the time of the last publish to now.
     */
    public void updateLastSeen() {
        lastSeenTimestampRef.set(Instant.now().toEpochMilli());
    }

    /**
     * Adds one to the count of page documents.
     */
    public void incrementPaginationDocumentCount() {
        paginationDocumentCountRef.incrementAndGet();
    }

    /**
     * Adds to the total size of page documents.
     *
     * @param documentSize the size, in bytes
     */
    public void addPaginationDocumentSize(int documentSize) {
        paginationDocumentSizeRef.addAndGet(documentSize);
    }

    /**
     * Records the write duration of a page document.
     *
     * @param durationInMillis the duration, in milliseconds
     */
    public void updateLastPaginationDocumentWriteDuration(long durationInMillis) {
        lastPaginationDocumentWriteDurationRef.set(durationInMillis);
        aggregatedPaginationDocumentWriteDurationRef.addAndGet(durationInMillis);
    }

    /**
     * Adds one to the count of buffered page entries.
     */
    public void incrementEntryBufferCount() {
        entryBufferCountRef.incrementAndGet();
    }

    /**
     * Adds to the total size of buffered page entries.
     *
     * @param documentSize the size, in bytes
     */
    public void addEntryBufferSize(int documentSize) {
        entryBufferSizeRef.addAndGet(documentSize);
    }

    /**
     * Records the write duration of a page entry.
     *
     * @param durationInMillis the duration, in milliseconds
     */
    public void updateLastEntryBufferWriteDuration(long durationInMillis) {
        lastEntryBufferWriteDurationRef.set(durationInMillis);
        aggregatedEntryBufferWriteDurationRef.addAndGet(durationInMillis);
    }

    /**
     * Adds one to the count of buffered documents.
     */
    public void incrementDocumentBufferCount() {
        documentBufferCountRef.incrementAndGet();
    }

    /**
     * Adds to the total size of buffered documents.
     *
     * @param documentSize the size, in bytes
     */
    public void addDocumentBufferSize(int documentSize) {
        documentBufferSizeRef.addAndGet(documentSize);
    }

    /**
     * Records the write duration of a document.
     *
     * @param durationInMillis the duration, in milliseconds
     */
    public void updateLastDocumentBufferWriteDuration(long durationInMillis) {
        lastDocumentBufferWriteDurationRef.set(durationInMillis);
        aggregatedDocumentBufferWriteDurationRef.addAndGet(durationInMillis);
    }

    /**
     * Adds to the count of published buffers.
     *
     * @param bufferCount the number of buffers
     */
    public void addPublishedBufferCount(int bufferCount) {
        publishedBufferCountRef.addAndGet(bufferCount);
    }

    /**
     * Sets the number of buffers of the last publish.
     *
     * @param bufferCount the number of buffers
     */
    public void updateLastPublishedBufferCount(int bufferCount) {
        lastPublishedBufferCountRef.set(bufferCount);
    }

    /**
     * Adds to the count of published positions.
     *
     * @param bufferCount the number of positions
     */
    public void addPublishedPositionCount(int bufferCount) {
        publishedPositionCountRef.addAndGet(bufferCount);
    }

    /**
     * Records the duration of a publish and adds one to the count of publishes.
     *
     * @param durationInMillis the duration, in milliseconds
     */
    public void updateLastPublishedPositionWriteDuration(long durationInMillis) {
        publishedCountRef.incrementAndGet();
        lastPublishWriteDurationRef.set(durationInMillis);
        aggregatedPublishWriteDurationRef.addAndGet(durationInMillis);
    }

    /**
     * A snapshot of the counters, with averages.
     *
     * @return the snapshot
     */
    public ContentStreamInfo build() {
        return new ContentStreamInfo(
                lastSeenTimestampRef.get() == 0L ? null : Instant.ofEpochMilli(lastSeenTimestampRef.get()).toString(),
                paginationDocumentCountRef.get(),
                Math.round(HealthResourceUtils.divide(paginationDocumentSizeRef.get(), paginationDocumentCountRef.get())),
                lastPaginationDocumentWriteDurationRef.get(),
                Math.round(HealthResourceUtils.divide(aggregatedPaginationDocumentWriteDurationRef.get(), paginationDocumentCountRef.get())),
                entryBufferCountRef.get(),
                Math.round(HealthResourceUtils.divide(entryBufferSizeRef.get(), entryBufferCountRef.get())),
                lastEntryBufferWriteDurationRef.get(),
                Math.round(HealthResourceUtils.divide(aggregatedEntryBufferWriteDurationRef.get(), entryBufferCountRef.get())),
                documentBufferCountRef.get(),
                Math.round(HealthResourceUtils.divide(documentBufferSizeRef.get(), documentBufferCountRef.get())),
                lastDocumentBufferWriteDurationRef.get(),
                Math.round(HealthResourceUtils.divide(aggregatedDocumentBufferWriteDurationRef.get(), documentBufferCountRef.get())),
                activePositionCountSupplier.get(),
                activeBufferCountSupplier.get(),
                publishedBufferCountRef.get(),
                lastPublishedBufferCountRef.get(),
                publishedPositionCountRef.get(),
                lastPublishWriteDurationRef.get(),
                Math.round(HealthResourceUtils.divide(aggregatedPublishWriteDurationRef.get(), publishedCountRef.get())));
    }

    /**
     * A snapshot of the counters, as the health endpoint renders it.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings("WeakerAccess")
    public static class ContentStreamInfo {
        /**
         * The time of the last publish (ISO-8601), or {@code null} if nothing is published.
         */
        @JsonProperty("last-seen")
        public final String lastSeenTimestamp;

        /**
         * The count of page documents.
         */
        @JsonProperty("page-document-count")
        public final Long paginationDocumentCount;
        /**
         * The average size of a page document, in bytes.
         */
        @JsonProperty("avg-page-document-size-bytes")
        public final Integer averagePaginationDocumentSize;
        /**
         * The write duration of the last page document, in milliseconds.
         */
        @JsonProperty("last-page-document-write-duration-millis")
        public final Long lastPaginationDocumentWriteDuration;
        /**
         * The average write duration of a page document, in milliseconds.
         */
        @JsonProperty("avg-page-document-write-duration-millis")
        public final Integer averagePaginationDocumentWriteDuration;

        /**
         * The count of buffered page entries.
         */
        @JsonProperty("page-entry-count")
        public final Long paginationEntryBufferCount;
        /**
         * The average size of a page entry, in bytes.
         */
        @JsonProperty("avg-page-entry-size-bytes")
        public final Integer averagePaginationEntryBufferSize;
        /**
         * The write duration of the last page entry, in milliseconds.
         */
        @JsonProperty("last-page-entry-write-duration-millis")
        public final Long lastEntryBufferWriteDuration;
        /**
         * The average write duration of a page entry, in milliseconds.
         */
        @JsonProperty("avg-page-entry-write-duration-millis")
        public final Integer averageEntryBufferWriteDuration;

        /**
         * The count of buffered documents.
         */
        @JsonProperty("entry-document-count")
        public final Long documentBufferCount;
        /**
         * The average size of a document, in bytes.
         */
        @JsonProperty("avg-entry-document-size-bytes")
        public final Integer averageDocumentBufferSize;
        /**
         * The write duration of the last document, in milliseconds.
         */
        @JsonProperty("last-entry-document-write-duration-millis")
        public final Long lastDocumentBufferWriteDuration;
        /**
         * The average write duration of a document, in milliseconds.
         */
        @JsonProperty("avg-entry-document-write-duration-millis")
        public final Integer averageDocumentBufferWriteDuration;

        /**
         * The number of positions buffered.
         */
        @JsonProperty("active-position-count")
        public final Integer activePositionCount;
        /**
         * The number of content buffers buffered.
         */
        @JsonProperty("active-buffer-count")
        public final Integer activeBufferCount;
        /**
         * The count of published buffers.
         */
        @JsonProperty("published-buffer-count")
        public final Long publishedBufferCount;
        /**
         * The number of buffers of the last publish.
         */
        @JsonProperty("last-published-buffer-count")
        public final Long lastPublishedBufferCount;
        /**
         * The count of published positions.
         */
        @JsonProperty("published-position-count")
        public final Long publishedPositionCount;
        /**
         * The duration of the last publish, in milliseconds.
         */
        @JsonProperty("last-published-position-write-duration-millis")
        public final Long lastPublishWriteDuration;
        /**
         * The average duration of a publish, in milliseconds.
         */
        @JsonProperty("avg-published-position-write-duration-millis")
        public final Integer averagePublishWriteDuration;

        ContentStreamInfo(String lastSeenTimestamp,
                          Long paginationDocumentCount,
                          Integer averagePaginationDocumentSize,
                          Long lastPaginationDocumentWriteDuration,
                          Integer averagePaginationDocumentWriteDuration,
                          Long paginationEntryBufferCount,
                          Integer averagePaginationEntryBufferSize,
                          Long lastEntryBufferWriteDuration,
                          Integer averageEntryBufferWriteDuration,
                          Long documentBufferCount,
                          Integer averageDocumentBufferSize,
                          Long lastDocumentBufferWriteDuration,
                          Integer averageDocumentBufferWriteDuration,
                          Integer activePositionCount,
                          Integer activeBufferCount,
                          Long publishedBufferCount,
                          Long lastPublishedBufferCount,
                          Long publishedPositionCount,
                          Long lastPublishWriteDuration,
                          Integer averagePublishWriteDuration
        ) {
            this.lastSeenTimestamp = lastSeenTimestamp;
            this.activePositionCount = activePositionCount;
            this.activeBufferCount = activeBufferCount;
            this.paginationDocumentCount = paginationDocumentCount;
            this.averagePaginationDocumentSize = averagePaginationDocumentSize;
            this.lastPaginationDocumentWriteDuration = lastPaginationDocumentWriteDuration;
            this.averagePaginationDocumentWriteDuration = averagePaginationDocumentWriteDuration;
            this.paginationEntryBufferCount = paginationEntryBufferCount;
            this.averagePaginationEntryBufferSize = averagePaginationEntryBufferSize;
            this.lastEntryBufferWriteDuration = lastEntryBufferWriteDuration;
            this.averageEntryBufferWriteDuration = averageEntryBufferWriteDuration;
            this.documentBufferCount = documentBufferCount;
            this.averageDocumentBufferSize = averageDocumentBufferSize;
            this.lastDocumentBufferWriteDuration = lastDocumentBufferWriteDuration;
            this.averageDocumentBufferWriteDuration = averageDocumentBufferWriteDuration;
            this.publishedBufferCount = publishedBufferCount;
            this.lastPublishedBufferCount = lastPublishedBufferCount;
            this.publishedPositionCount = publishedPositionCount;
            this.lastPublishWriteDuration = lastPublishWriteDuration;
            this.averagePublishWriteDuration = averagePublishWriteDuration;
        }
    }
}
