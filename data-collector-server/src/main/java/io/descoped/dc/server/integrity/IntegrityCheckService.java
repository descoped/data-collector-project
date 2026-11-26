package io.descoped.dc.server.integrity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.application.spi.Service;
import io.descoped.dc.server.content.ContentStoreComponent;
import io.descoped.dc.server.db.LmdbEnvironment;
import io.descoped.dc.server.db.SequenceDbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.descoped.dc.server.db.SequenceDbHelper.getSequenceDatabaseLocation;

/**
 * Runs integrity checks of topics, one job per topic.
 */
public class IntegrityCheckService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityCheckService.class);

    private static final String CLOSED = "CLOSED";

    final DynamicConfiguration configuration;
    final ContentStoreComponent contentStoreComponent;
    final Map<String, IntegrityCheckJob> jobs = new ConcurrentHashMap<>();
    // the topics whose job is starting or running, claimed before anything is deleted or opened (#67)
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    // the topics asked to stop while their job was starting: the job stops itself when it registers
    private final Set<String> cancelWhenRegistered = ConcurrentHashMap.newKeySet();

    /**
     * Creates the service.
     *
     * @param configuration the configuration
     * @param contentStoreComponent the content store
     */
    public IntegrityCheckService(DynamicConfiguration configuration, ContentStoreComponent contentStoreComponent) {
        this.configuration = configuration;
        this.contentStoreComponent = contentStoreComponent;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void start() {
        // a job starts on request (createJob)
    }

    @Override
    public void stop() {
        for (Map.Entry<String, IntegrityCheckJob> entry : jobs.entrySet()) {
            entry.getValue().terminate();
        }
    }

    /**
     * Whether a topic has a running or a completed job.
     *
     * @param topic the topic
     * @return true if it has
     */
    public boolean hasJob(String topic) {
        if (jobs.containsKey(topic)) {
            return true;
        }

        List<JobStatus> previouslyCompletedJobs = getPreviouslyCompletedJobs();
        return previouslyCompletedJobs.contains(new JobStatus(topic, CLOSED));
    }

    /**
     * Whether a topic's job runs.
     *
     * @param topic the topic
     * @return true if it runs
     */
    public boolean isJobRunning(String topic) {
        if (active.contains(topic)) {
            return true;
        }
        IntegrityCheckJob job = jobs.get(topic);
        return job != null && "RUNNING".equals(job.getSummary().status);
    }

    /**
     * Removes a topic's job if it has ended.
     *
     * @param topic the topic
     * @return true if it was removed
     */
    public boolean removeJobIfClosed(String topic) {
        if (jobs.containsKey(topic) && !"RUNNING".equals(jobs.get(topic).getSummary().status)) {
            jobs.remove(topic);
            return true;
        }
        return false;
    }

    /**
     * Starts a job for a topic, replacing the topic's earlier index, unless the topic's job is starting or running.
     *
     * @param topic the topic
     * @return true if the job started, false if the topic's job is starting or running
     */
    public boolean createJob(String topic) {
        if (isJobRunning(topic) || !active.add(topic)) {
            return false;
        }
        boolean started = false;
        try {
            startJob(topic);
            started = true;
        } finally {
            if (!started) {
                active.remove(topic);
            }
        }
        return true;
    }

    private void startJob(String topic) {
        Path dbLocation = getSequenceDatabaseLocation(configuration);
        LOG.trace("Database path: {}", dbLocation);

        LmdbEnvironment.removePath(SequenceDbHelper.topicDirectory(dbLocation, topic));

        CompletableFuture.supplyAsync(() -> {
            try (LmdbEnvironment lmdbEnvironment = new LmdbEnvironment(configuration, dbLocation, topic)) {
                try (IntegrityCheckIndex index = new IntegrityCheckIndex(lmdbEnvironment)) {
                    IntegrityCheckJobSummary summary = new IntegrityCheckJobSummary();
                    IntegrityCheckJob job = new IntegrityCheckJob(configuration, contentStoreComponent, index, summary);
                    jobs.put(topic, job);
                    if (cancelWhenRegistered.remove(topic)) {
                        job.terminate();
                    }
                    job.consume(topic);
                    LOG.info("Check integrity of topic {}\u00A0completed successfully at position {}!", topic, summary.getLastPosition());
                    return job;
                }
            }
        }).whenComplete((job, throwable) -> {
            cancelWhenRegistered.remove(topic);
            active.remove(topic);
        }).exceptionally(throwable -> {
            LOG.error("Ended exceptionally with error: {}", CommonUtils.captureStackTrace(throwable));
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (throwable instanceof Error error) {
                throw error;
            } else {
                throw new RuntimeException(throwable);
            }
        });

        int failCount = 10;
        while (jobs.get(topic) == null) {
            try {
                if (failCount == 0) break;
                TimeUnit.MILLISECONDS.sleep(5);
                failCount--;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The running and the completed jobs.
     *
     * @return the jobs
     */
    public List<JobStatus> getJobs() {
        List<JobStatus> jobStatusList = jobs.values().stream()
                .map(job -> {
                    IntegrityCheckJobSummary.Summary summary = job.getSummary();
                    return new JobStatus(summary.topic, summary.status);
                }).toList();

        return merge(jobStatusList, getPreviouslyCompletedJobs());
    }

    // the jobs held in memory, then each completed job on disk that is not among them
    static List<JobStatus> merge(List<JobStatus> jobStatusList, List<JobStatus> previouslyCompletedJobs) {
        List<JobStatus> merged = new ArrayList<>(jobStatusList);
        for (JobStatus previousCompletedJob : previouslyCompletedJobs) {
            if (!merged.contains(previousCompletedJob)) {
                merged.add(previousCompletedJob);
            }
        }
        return merged;
    }

    List<JobStatus> getPreviouslyCompletedJobs() {
        try {
            Path dbLocation = getSequenceDatabaseLocation(configuration);
            try (Stream<Path> paths = Files.list(dbLocation)) {
                return paths.filter(path -> path.toFile().isDirectory())
                        .map(path -> new JobStatus(path.getFileName().toString(), CLOSED)).toList();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A topic's job summary.
     *
     * @param topic the topic
     * @return the summary
     */
    public IntegrityCheckJobSummary.Summary getJobSummary(String topic) {
        if (jobs.containsKey(topic)) {
            return jobs.get(topic).getSummary();
        }

        // try load summary from filesystem if exists
        List<JobStatus> jobStatusList = getJobs();
        if (!jobStatusList.contains(new JobStatus(topic, CLOSED))) {
            throw new IllegalStateException("Job not found for topic: " + topic);
        }

        Path summaryPath = SequenceDbHelper.topicDirectory(getSequenceDatabaseLocation(configuration), topic).resolve("report").resolve("summary.json");
        if (!summaryPath.toFile().exists()) {
            return null;
        }
        try {
            String summaryJson = Files.readString(summaryPath);
            return JsonParser.createJsonParser().fromJson(summaryJson, IntegrityCheckJobSummary.Summary.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Path writeJobSummaryReport(String topic, Path summaryReportFilePath, IntegrityCheckJobSummary.Summary summary) {
        /*
         * The check-integrity job updates the summary metrics and is kept in memory (Service.jobs).
         * The summary object contains a report-path and report-id that points a file containing duplicates (json-array).
         */

        Path reportFilePath = summary.reportPath.resolve(summary.reportId);
        JsonParser jsonParser = JsonParser.createJsonParser();
        String jsonSummaryResponseBody = jsonParser.toPrettyJSON(summary);

        try (BufferedReader jsonSummaryReader = new BufferedReader(new StringReader(jsonSummaryResponseBody))) {
            try (FileWriter jsonSummaryFileWriter = new FileWriter(summaryReportFilePath.toFile(), true)) {
                try (BufferedWriter jsonSummaryBufferedWriter = new BufferedWriter(jsonSummaryFileWriter)) {
                    jsonSummaryBufferedWriter.write("{");
                    jsonSummaryBufferedWriter.newLine();
                    String jsonSummaryLine = jsonSummaryReader.readLine();
                    // the summary's opening line is skipped, and its closing brace ends the copy
                    if (jsonSummaryLine != null) {
                        jsonSummaryLine = jsonSummaryReader.readLine();
                    }
                    while (jsonSummaryLine != null && !"}".equals(jsonSummaryLine)) {
                        jsonSummaryBufferedWriter.write(jsonSummaryLine);
                        jsonSummaryBufferedWriter.newLine();

                        jsonSummaryLine = jsonSummaryReader.readLine();
                    }

                    // write full summary
                    jsonSummaryBufferedWriter.write(" ,\"duplicates\" : ");
                    try (BufferedReader jsonReportReader = new BufferedReader(new FileReader(reportFilePath.toFile()))) {
                        String jsonReportLine = jsonReportReader.readLine();
                        boolean skippedReportLine = false;
                        while (jsonReportLine != null) {
                            if (skippedReportLine) {
                                jsonSummaryBufferedWriter.write("    ");
                            }
                            if (!skippedReportLine) {
                                skippedReportLine = true;
                            }
                            jsonSummaryBufferedWriter.write(jsonReportLine);
                            jsonSummaryBufferedWriter.newLine();
                            jsonReportLine = jsonReportReader.readLine();
                        }
                    }

                    jsonSummaryBufferedWriter.write("}");
                    jsonSummaryBufferedWriter.newLine();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return reportFilePath;
    }

    /**
     * Cancels a topic's job.
     *
     * @param topic the topic
     */
    public void cancelJob(String topic) {
        if (!isJobRunning(topic)) {
            return;
        }
        IntegrityCheckJob job = jobs.get(topic);
        if (job != null && "RUNNING".equals(job.getSummary().status)) {
            job.terminate();
        } else if (active.contains(topic)) {
            // still opening its index: the job stops itself when it registers; checked again in case it registered
            // in between, and terminating twice is harmless
            cancelWhenRegistered.add(topic);
            IntegrityCheckJob registered = jobs.get(topic);
            if (registered != null && registered != job) {
                registered.terminate();
            }
        }
    }

    /**
     * A job's topic and status.
     */
    public static class JobStatus {
        @JsonProperty
        String topic;
        @JsonProperty
        String status;

        /**
         * Creates the status.
         *
         * @param topic  the topic
         * @param status the status
         */
        public JobStatus(String topic, String status) {
            this.topic = topic;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JobStatus jobStatus = (JobStatus) o;
            return Objects.equals(topic, jobStatus.topic) &&
                    Objects.equals(status, jobStatus.status);
        }

        @Override
        public int hashCode() {
            return Objects.hash(topic, status);
        }
    }

}
