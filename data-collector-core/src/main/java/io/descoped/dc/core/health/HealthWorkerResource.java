package io.descoped.dc.core.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.health.HealthResourceExclude;
import io.descoped.dc.core.executor.WorkerStatus;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The health resource {@code worker-<id>} of one worker: its status and its monitor's snapshot. It is up when the
 * monitor's content stream is up.
 */
@HealthRenderPriority(priority = 10)
@HealthResourceExclude
public class HealthWorkerResource implements HealthResource {

    private final UUID workerId;
    private final HealthWorkerMonitor monitor;

    /**
     * Creates the resource with a new monitor.
     *
     * @param workerId the worker's id
     */
    public HealthWorkerResource(UUID workerId) {
        this.workerId = workerId;
        monitor = new HealthWorkerMonitor();
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.of(monitor.contentStream().monitor().isUp());
    }

    @Override
    public String name() {
        return "worker-" + workerId.toString();
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        return new WorkerResource(workerId.toString(), monitor.build());
    }

    /**
     * The worker's monitor.
     *
     * @return the monitor
     */
    public HealthWorkerMonitor getMonitor() {
        return monitor;
    }

    /**
     * The rendered resource of a worker.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"status", "worker-id"})
    @SuppressWarnings("WeakerAccess")
    public static class WorkerResource {
        /**
         * The worker's status.
         */
        @JsonProperty("status")
        public final WorkerStatus status;
        /**
         * The worker's id.
         */
        @JsonProperty("worker-id")
        public final String workerId;
        /**
         * The monitor's snapshot, rendered inline.
         */
        @JsonUnwrapped
        public final HealthWorkerMonitor.WorkerInfo workerInfo;

        /**
         * Creates the resource; the status is taken from the snapshot.
         *
         * @param workerId   the worker's id
         * @param workerInfo the monitor's snapshot
         */
        public WorkerResource(String workerId, HealthWorkerMonitor.WorkerInfo workerInfo) {
            this.workerId = workerId;
            this.workerInfo = workerInfo;
            this.status = workerInfo.status;
        }
    }

}
