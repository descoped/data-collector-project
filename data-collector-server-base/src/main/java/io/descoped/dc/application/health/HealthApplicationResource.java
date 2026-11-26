package io.descoped.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;

import java.util.Deque;
import java.util.Map;
import java.util.Optional;

/**
 * The health output of the server itself.
 */
@HealthRenderPriority(priority = 1)
public class HealthApplicationResource implements HealthResource {

    private final HealthApplicationMonitor monitor;

    /**
     * Creates the resource.
     */
    public HealthApplicationResource() {
        monitor = new HealthApplicationMonitor();
    }

    @Override
    public Optional<Boolean> isUp() {
        return Optional.of(monitor.getServerStatus() == HealthApplicationMonitor.ServerStatus.RUNNING);
    }

    @Override
    public String name() {
        return "application";
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
        return new ServerInfo(
                monitor.getServerStatus().name(),
                monitor.getHost(),
                monitor.getPort(),
                monitor.getSince(),
                monitor.getNow(),
                monitor.getUptime());
    }

    /**
     * The monitor the resource reports.
     *
     * @return the monitor
     */
    public HealthApplicationMonitor getMonitor() {
        return monitor;
    }

    /**
     * The server's health output.
     */
    @SuppressWarnings("WeakerAccess")
    public static class ServerInfo {
        /**
         * The status.
         */
        @JsonProperty
        public final String status;
        /**
         * The host.
         */
        @JsonProperty
        public final String host;
        /**
         * The port.
         */
        @JsonProperty
        public final Integer port;
        /**
         * When the status was set.
         */
        @JsonProperty
        public final String since;
        /**
         * The current time.
         */
        @JsonProperty
        public final String now;
        /**
         * The time since the status was set.
         */
        @JsonProperty
        public final String uptime;

        /**
         * Creates the output.
         *
         * @param status the status
         * @param host   the host
         * @param port   the port
         * @param since  when the status was set
         * @param now    the current time
         * @param uptime the time since the status was set
         */
        public ServerInfo(String status, String host, Integer port, String since, String now, String uptime) {
            this.status = status;
            this.host = host;
            this.port = port;
            this.since = since;
            this.now = now;
            this.uptime = uptime;
        }
    }
}
