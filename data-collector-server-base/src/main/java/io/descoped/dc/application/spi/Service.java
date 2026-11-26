package io.descoped.dc.application.spi;

/**
 * A service of the application, discovered and started with it.
 */
public interface Service {

    /**
     * Whether the service is enabled.
     *
     * @return true if it is
     */
    boolean isEnabled();

    /**
     * Starts the service.
     */
    void start();

    /**
     * Stops the service.
     */
    void stop();

}
