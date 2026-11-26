package io.descoped.dc.application.health;

import io.descoped.dc.api.health.HealthResourceUtils;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The server's status, host, port and start time, for the health output.
 */
public class HealthApplicationMonitor {

    /**
     * Creates the monitor; the status is {@link ServerStatus#SHUTDOWN}.
     */
    public HealthApplicationMonitor() {
        // the state is in the fields
    }

    private final AtomicReference<ServerStatus> serverStatus = new AtomicReference<>(ServerStatus.SHUTDOWN);
    private final AtomicReference<String> host = new AtomicReference<>();
    private final AtomicInteger port = new AtomicInteger();
    private final AtomicLong since = new AtomicLong(0);

    /**
     * The server's status.
     *
     * @return the status
     */
    public ServerStatus getServerStatus() {
        return serverStatus.get();
    }

    /**
     * Sets the server's status.
     *
     * @param status the status
     */
    public void setServerStatus(ServerStatus status) {
        serverStatus.set(status);
        if (ServerStatus.RUNNING == status) {
            since.set(Instant.now().toEpochMilli());
        }
    }

    /**
     * The server's host.
     *
     * @return the host
     */
    public String getHost() {
        return host.get();
    }

    /**
     * Sets the server's host.
     *
     * @param host the host
     */
    public void setHost(String host) {
        this.host.set(host);
    }

    /**
     * The server's port.
     *
     * @return the port
     */
    public Integer getPort() {
        return port.get();
    }

    /**
     * Sets the server's port.
     *
     * @param port the port
     */
    public void setPort(Integer port) {
        this.port.set(port);
    }

    /**
     * When the status was last set, as an ISO-8601 instant.
     *
     * @return the time
     */
    public String getSince() {
        return Instant.ofEpochMilli(since.get()).toString();
    }

    /**
     * The current time, as an ISO-8601 instant.
     *
     * @return the time
     */
    public String getNow() {
        return Instant.ofEpochMilli(Instant.now().toEpochMilli()).toString();
    }

    /**
     * The time since the status was last set.
     *
     * @return the duration, as text
     */
    public String getUptime() {
        return HealthResourceUtils.durationAsString(since.get());
    }

    /**
     * The server's status.
     */
    public enum ServerStatus {
        /**
         * Being initialized.
         */
        INITIALIZING,
        /**
         * Initialized, not started.
         */
        INITIALIZED,
        /**
         * Started.
         */
        RUNNING,
        /**
         * Stopping.
         */
        SHUTTING_DOWN,
        /**
         * Stopped.
         */
        SHUTDOWN
    }

}
