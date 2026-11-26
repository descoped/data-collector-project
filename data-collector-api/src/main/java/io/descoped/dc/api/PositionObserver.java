package io.descoped.dc.api;

import java.util.function.Consumer;

/**
 * Tells a pagination how many positions a page expects and how many are completed.
 */
public class PositionObserver {

    private final Consumer<Integer> expectedCallback;
    private final Consumer<Integer> completedCallback;

    /**
     * Creates the observer.
     *
     * @param expectedCallback  called with the count of expected positions
     * @param completedCallback called with the count of completed positions
     */
    public PositionObserver(Consumer<Integer> expectedCallback, Consumer<Integer> completedCallback) {
        this.expectedCallback = expectedCallback;
        this.completedCallback = completedCallback;
    }

    /**
     * Reports expected positions.
     *
     * @param count the count
     */
    public void expected(int count) {
        expectedCallback.accept(count);
    }

    /**
     * Reports completed positions.
     *
     * @param count the count
     */
    public void completed(int count) {
        completedCallback.accept(count);
    }
}
