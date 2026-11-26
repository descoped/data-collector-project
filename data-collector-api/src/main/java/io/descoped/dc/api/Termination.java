package io.descoped.dc.api;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A flag that tells a running execution to stop.
 */
public class Termination {

    private final AtomicBoolean terminated = new AtomicBoolean(false);

    private Termination() {
    }

    /**
     * Sets the flag.
     */
    public void terminate() {
        terminated.set(true);
    }

    /**
     * Whether the flag is set.
     *
     * @return {@code true} once {@link #terminate()} is called
     */
    public boolean isTerminated() {
        return terminated.get();
    }

    /**
     * Creates an unset flag.
     *
     * @return the flag
     */
    public static Termination create() {
        return new Termination();
    }
}
