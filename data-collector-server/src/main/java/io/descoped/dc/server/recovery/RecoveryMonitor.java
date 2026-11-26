package io.descoped.dc.server.recovery;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthResourceUtils;

import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Counts a recovery's progress, and builds its summary.
 */
public class RecoveryMonitor {

    /**
     * Creates the monitor.
     */
    public RecoveryMonitor() {
        // the state is in the fields
    }

    final AtomicBoolean running = new AtomicBoolean();
    final AtomicLong started = new AtomicLong();
    final AtomicLong ended = new AtomicLong();
    final AtomicReference<String> startPosition = new AtomicReference<>();
    final AtomicReference<String> currentPosition = new AtomicReference<>();
    final AtomicReference<String> lastPosition = new AtomicReference<>();
    final AtomicReference<Path> sourceDatabasePath = new AtomicReference<>();
    final AtomicReference<String> sourceTopic = new AtomicReference<>();
    final AtomicReference<String> targetTopic = new AtomicReference<>();
    final AtomicLong bufferedPositions = new AtomicLong(0);
    final AtomicLong copiedPositions = new AtomicLong(0);
    final AtomicLong postCheckFromTimestamp = new AtomicLong(0);
    final AtomicReference<String> postCheckStartPosition = new AtomicReference<>();
    final AtomicReference<String> postCheckLastPosition = new AtomicReference<>();
    final AtomicLong postCheckCheckedPositions = new AtomicLong(0);

    void setStarted() {
        started.set(System.currentTimeMillis());
        running.set(true);
    }

    void setEnded() {
        ended.set(System.currentTimeMillis());
        running.set(false);
    }

    void setStartPosition(String position) {
        startPosition.set(position);
    }

    void setCurrentPosition(String position) {
        currentPosition.set(position);
    }

    void setLastPosition(String position) {
        lastPosition.set(position);
    }

    void setSourceDatabasePath(Path dbLocation) {
        sourceDatabasePath.set(dbLocation);
    }

    void setSourceTopic(String sourceTopic) {
        this.sourceTopic.set(sourceTopic);
    }

    void setTargetTopic(String targetTopic) {
        this.targetTopic.set(targetTopic);
    }

    void incrementBufferedPositions() {
        bufferedPositions.incrementAndGet();
    }

    void resetBufferedPositions() {
        bufferedPositions.set(0);
    }

    void incrementCopiedPositions(int numberOfPositions) {
        copiedPositions.addAndGet(numberOfPositions);
    }

    void setPostCheckFromTimestamp(long timestamp) {
        postCheckFromTimestamp.set(timestamp);
    }

    void setPostCheckStartPosition(String position) {
        postCheckStartPosition.set(position);
    }

    void setPostCheckLastPosition(String position) {
        postCheckLastPosition.set(position);
    }

    void incrementPostCheckCheckedPositions() {
        postCheckCheckedPositions.incrementAndGet();
    }

    /**
     * Builds the summary.
     *
     * @return the summary
     */
    public Summary build() {
        return new Summary(
                running.get(),
                started.get(),
                ended.get(),
                startPosition.get(),
                currentPosition.get(),
                lastPosition.get(),
                sourceDatabasePath.get(),
                sourceTopic.get(),
                targetTopic.get(),
                bufferedPositions.get(),
                copiedPositions.get(),
                postCheckFromTimestamp.get(),
                postCheckStartPosition.get(),
                postCheckLastPosition.get(),
                postCheckCheckedPositions.get()
        );
    }

    /**
     * A recovery's summary, as served on {@code /recovery}.
     */
    public static class Summary {

        /**
         * The status.
         */
        @JsonProperty
        public final String status;
        /**
         * When the recovery started.
         */
        @JsonProperty
        public final String started;
        /**
         * When it ended.
         */
        @JsonProperty
        public final String ended;
        /**
         * How long it has run.
         */
        @JsonProperty
        public final String since;
        /**
         * The first position copied.
         */
        @JsonProperty
        public final String startPosition;
        /**
         * The position being copied.
         */
        @JsonProperty
        public final String currentPosition;
        /**
         * The last position copied.
         */
        @JsonProperty
        public final String lastPosition;
        /**
         * The source topic's index.
         */
        @JsonProperty
        public final Path indexDatabasePath;
        /**
         * The topic copied from.
         */
        @JsonProperty
        public final String sourceTopic;
        /**
         * The topic copied to.
         */
        @JsonProperty
        public final String targetTopic;
        /**
         * The number of positions read from the index.
         */
        @JsonProperty
        public final long bufferedPositions;
        /**
         * The number of positions copied.
         */
        @JsonProperty
        public final long copiedPositions;
        /**
         * The positions copied per second.
         */
        @JsonProperty
        public final float averageCopiedPositionsPerSecond;
        /**
         * When the check of messages written during the copy starts from.
         */
        @JsonProperty
        public final String postCheckFromTimestamp;
        /**
         * The first position the check found.
         */
        @JsonProperty
        public final String postCheckStartPosition;
        /**
         * The last position the check found.
         */
        @JsonProperty
        public final String postCheckLastPosition;
        /**
         * The number of positions the check found.
         */
        @JsonProperty
        public final long postCheckCheckedPositions;

        /**
         * Creates the summary.
         *
         * @param running                   whether the recovery runs
         * @param started                   when it started, in epoch milliseconds
         * @param ended                     when it ended, in epoch milliseconds
         * @param startPosition             the first position copied
         * @param currentPosition           the position being copied
         * @param lastPosition              the last position copied
         * @param sourceDatabasePath        the source topic's index
         * @param sourceTopic               the topic copied from
         * @param targetTopic               the topic copied to
         * @param bufferedPositions         the number of positions read from the index
         * @param copiedPositions           the number of positions copied
         * @param postCheckFromTimestamp    when the check of messages written during the copy starts from, in epoch milliseconds
         * @param postCheckStartPosition    the first position the check found
         * @param postCheckLastPosition     the last position the check found
         * @param postCheckCheckedPositions the number of positions the check found
         */
        public Summary(boolean running,
                       long started,
                       long ended,
                       String startPosition,
                       String currentPosition,
                       String lastPosition,
                       Path sourceDatabasePath,
                       String sourceTopic,
                       String targetTopic,
                       long bufferedPositions,
                       long copiedPositions,
                       long postCheckFromTimestamp,
                       String postCheckStartPosition,
                       String postCheckLastPosition,
                       long postCheckCheckedPositions) {

            this.status = running ? "RUNNING" : "COMPLETED";
            this.started = Instant.ofEpochMilli(started).toString();
            this.ended = Instant.ofEpochMilli(ended).toString();
            this.since = HealthResourceUtils.durationAsString(started);
            this.startPosition = startPosition;
            this.currentPosition = currentPosition;
            this.lastPosition = lastPosition;
            this.indexDatabasePath = sourceDatabasePath;
            this.sourceTopic = sourceTopic;
            this.targetTopic = targetTopic;
            this.bufferedPositions = bufferedPositions;
            this.copiedPositions = copiedPositions;
            this.postCheckFromTimestamp = Instant.ofEpochMilli(postCheckFromTimestamp).toString();
            this.postCheckStartPosition = postCheckStartPosition;
            this.postCheckLastPosition = postCheckLastPosition;
            this.postCheckCheckedPositions = postCheckCheckedPositions;

            long now = System.currentTimeMillis();
            Float averageRequestPerSecond = HealthResourceUtils.divide(copiedPositions, (now - started) / 1000);
            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.UP);
            this.averageCopiedPositionsPerSecond = Float.parseFloat(df.format(averageRequestPerSecond));
        }
    }
}
