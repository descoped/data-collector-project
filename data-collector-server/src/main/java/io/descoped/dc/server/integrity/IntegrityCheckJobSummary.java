package io.descoped.dc.server.integrity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthResourceUtils;

import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Counts what an integrity check finds, and builds its summary.
 */
public class IntegrityCheckJobSummary {

    private final AtomicReference<String> topic = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong started = new AtomicLong();
    private final AtomicLong ended = new AtomicLong();
    private final AtomicReference<String> firstPosition = new AtomicReference<>();
    private final AtomicReference<String> lastPosition = new AtomicReference<>();
    private final AtomicReference<String> currentPosition = new AtomicReference<>();
    private final AtomicLong positionCount = new AtomicLong();
    private final AtomicReference<Path> duplicateReportPath = new AtomicReference<>();
    private final AtomicReference<String> duplicateReportId = new AtomicReference<>();
    private final AtomicLong duplicatePositions = new AtomicLong();
    private final AtomicLong affectedPositions = new AtomicLong();

    /**
     * Creates an empty summary.
     */
    public IntegrityCheckJobSummary() {
        // the values are set while the job runs
    }

    IntegrityCheckJobSummary setTopic(String topic) {
        this.topic.set(topic);
        return this;
    }

    IntegrityCheckJobSummary setStarted() {
        running.set(true);
        started.set(Instant.now().toEpochMilli());
        return this;
    }

    IntegrityCheckJobSummary setEnded() {
        ended.set(Instant.now().toEpochMilli());
        running.set(false);
        return this;
    }

    IntegrityCheckJobSummary setFirstPosition(String position) {
        firstPosition.set(position);
        return this;
    }

    /**
     * The last position indexed.
     *
     * @return the position
     */
    public String getLastPosition() {
        return lastPosition.get();
    }

    IntegrityCheckJobSummary setLastPosition(String position) {
        lastPosition.set(position);
        return this;
    }

    /**
     * The position being indexed.
     *
     * @return the position
     */
    public String getCurrentPosition() {
        return currentPosition.get();
    }

    IntegrityCheckJobSummary setCurrentPosition(String position) {
        currentPosition.set(position);
        return this;
    }

    IntegrityCheckJobSummary incrementPositionCount() {
        positionCount.incrementAndGet();
        return this;
    }

    /**
     * Sets where the report is written.
     *
     * @param reportPath the directory
     * @return this summary
     */
    public IntegrityCheckJobSummary setReportPath(Path reportPath) {
        duplicateReportPath.set(reportPath);
        return this;
    }

    /**
     * Sets the report's id.
     *
     * @param reportId the id
     * @return this summary
     */
    public IntegrityCheckJobSummary setDuplicateReportId(String reportId) {
        duplicateReportId.set(reportId);
        return this;
    }

    /**
     * Sets the duplicate count of each position.
     *
     * @param duplicatePositionCounter the counts by position
     * @return this summary
     */
    public IntegrityCheckJobSummary setDuplicatePositionStats(Map<String, AtomicLong> duplicatePositionCounter) {
        AtomicLong duplicatePositionCount = new AtomicLong(0);
        duplicatePositionCounter.forEach((key, value) -> {
            duplicatePositionCount.addAndGet(value.get());
        });
        duplicatePositions.set(duplicatePositionCount.get());
        affectedPositions.set(duplicatePositionCounter.size());
        return this;
    }

    /**
     * Builds the summary.
     *
     * @return the summary
     */
    public Summary build() {
        return new Summary(
                topic.get(),
                running.get(),
                started.get(),
                ended.get(),
                firstPosition.get(),
                lastPosition.get(),
                currentPosition.get(),
                positionCount.get(),
                duplicatePositions.get(),
                affectedPositions.get(),
                duplicateReportPath.get(),
                duplicateReportId.get()
        );
    }

    /**
     * An integrity check's summary, as served on {@code /check-integrity}.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Summary {
        /**
         * The topic.
         */
        @JsonProperty
        public String topic;
        /**
         * The status.
         */
        @JsonProperty
        public String status;
        /**
         * When the check started.
         */
        @JsonProperty
        public String started;
        /**
         * When the check ended.
         */
        @JsonProperty
        public String ended;
        /**
         * How long the check has run.
         */
        @JsonProperty
        public String since;
        /**
         * The first position.
         */
        @JsonProperty
        public String firstPosition;
        /**
         * The last position.
         */
        @JsonProperty
        public String lastPosition;
        /**
         * The position being checked.
         */
        @JsonProperty
        public String currentPosition;
        /**
         * The number of positions checked.
         */
        @JsonProperty
        public long checkedPositions;
        /**
         * The positions checked per second.
         */
        @JsonProperty
        public float averageCheckedPositionsPerSecond;
        /**
         * The number of duplicate positions.
         */
        @JsonProperty
        public long duplicatePositions;
        /**
         * The number of positions that have duplicates.
         */
        @JsonProperty
        public long affectedPositions;
        /**
         * Where the report is written.
         */
        @JsonIgnore
        public final Path reportPath;
        /**
         * The report's id.
         */
        @JsonIgnore
        public final String reportId;

        /**
         * Creates the summary.
         *
         * @param topic               the topic
         * @param running             whether the check runs
         * @param started             when it started, in epoch milliseconds
         * @param ended               when it ended, in epoch milliseconds
         * @param firstPosition       the first position
         * @param lastPosition        the last position
         * @param currentPosition     the position being checked
         * @param checkedPositions    the number of positions checked
         * @param duplicatePositions  the number of duplicate positions
         * @param affectedPositions   the number of positions that have duplicates
         * @param reportPath          where the report is written
         * @param reportId            the report's id
         */
        public Summary(String topic, boolean running, long started, long ended,
                       String firstPosition, String lastPosition, String currentPosition, long checkedPositions,
                       long duplicatePositions, long affectedPositions, Path reportPath, String reportId) {
            this.topic = topic;
            this.status = running ? "RUNNING" : "CLOSED";
            this.started = Instant.ofEpochMilli(started).toString();
            this.ended = Instant.ofEpochMilli(ended).toString();
            this.since = HealthResourceUtils.durationAsString(started);
            this.firstPosition = firstPosition;
            this.lastPosition = lastPosition;
            this.currentPosition = currentPosition;
            this.checkedPositions = checkedPositions;
            this.duplicatePositions = duplicatePositions;
            this.affectedPositions = affectedPositions;
            this.reportPath = reportPath;
            this.reportId = reportId;

            long now = System.currentTimeMillis();
            Float averageRequestPerSecond = HealthResourceUtils.divide(checkedPositions, (now - started) / 1000);
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.UP);
            this.averageCheckedPositionsPerSecond = Float.parseFloat(df.format(averageRequestPerSecond));
        }
    }

    /**
     * A position that has duplicates, with the ids of its messages.
     */
    public static class PositionSummary {
        @JsonProperty
        String position;
        @JsonProperty
        Integer duplicateCount;
        @JsonProperty
        List<String> ulid; // the JSON report keeps its 2.x field name; the values are message ids (UUIDv7)

        /**
         * Creates the entry.
         *
         * @param position       the position
         * @param duplicateCount the number of duplicates
         * @param ulid           the ids of its messages
         */
        public PositionSummary(String position, Integer duplicateCount, List<String> ulid) {
            this.position = position;
            this.duplicateCount = duplicateCount;
            this.ulid = ulid;
        }
    }

}
