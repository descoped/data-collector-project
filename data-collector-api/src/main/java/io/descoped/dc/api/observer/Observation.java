package io.descoped.dc.api.observer;

/**
 * The observation of something that takes time, such as a node's execution: a timer for metrics, a span for traces.
 * It is closed once, when the work ends.
 */
public interface Observation extends AutoCloseable {

    /**
     * The observation that observes nothing.
     */
    Observation NONE = new Observation() {
    };

    /**
     * The work failed; called before {@link #close()}.
     *
     * @param failure what was thrown
     */
    default void failed(Throwable failure) {
    }

    /**
     * The work ended.
     */
    @Override
    default void close() {
    }
}
