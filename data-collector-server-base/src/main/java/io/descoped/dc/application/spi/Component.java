package io.descoped.dc.application.spi;

/**
 * A component of the application, discovered as a service and closed when the application stops.
 */
public interface Component extends AutoCloseable {

    /**
     * Initializes the component.
     */
    void initialize();

    /**
     * Whether the component is open.
     *
     * @return true until it is closed
     */
    boolean isOpen();

    /**
     * The object the component wraps.
     *
     * @param <R> the delegate's type
     * @return the delegate
     */
    <R> R getDelegate();

}
