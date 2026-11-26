package io.descoped.dc.api.observer;

/**
 * Observes the engine. A provider is registered as a service of this interface ({@code provides} in its module, and
 * {@code META-INF/services}); the engine calls every provider it finds.
 * <p>
 * The engine calls an observer synchronously, on the thread that does the work, so an observer must not block. A
 * {@link RuntimeException} an observer throws is logged and never fails the engine; an {@link Error} is not caught.
 * Every method does nothing by default, so a
 * provider overrides what it needs. The events carry no headers, bodies or configuration values.
 */
public interface EngineObserver {

    /**
     * A worker starts.
     *
     * @param worker the worker
     */
    default void workerStarted(WorkerEvent worker) {
    }

    /**
     * A worker finished.
     *
     * @param worker the worker
     * @param status how it ended: {@code COMPLETED}, {@code CANCELED} or {@code FAILED}
     */
    default void workerFinished(WorkerEvent worker, String status) {
    }

    /**
     * A node starts to execute.
     *
     * @param node the node
     * @return the observation of the execution, closed when it ends
     */
    default Observation nodeStarted(NodeEvent node) {
        return Observation.NONE;
    }

    /**
     * An HTTP request is sent.
     *
     * @param request the request
     * @return the observation of the request, closed when it is answered or fails
     */
    default RequestObservation requestStarted(RequestEvent request) {
        return RequestObservation.NONE;
    }

    /**
     * A request failed and is tried again.
     *
     * @param request the request
     * @param attempt the attempt that failed, from 1
     */
    default void requestRetried(RequestEvent request, int attempt) {
    }

    /**
     * A GET step failed: its request could not be sent, or its response was not accepted.
     *
     * @param request the request of the step
     * @param failure what the step threw
     */
    default void operationFailed(RequestEvent request, Throwable failure) {
    }

    /**
     * A paginate node completed a page.
     *
     * @param paginate the paginate node
     */
    default void pageCompleted(NodeEvent paginate) {
    }

    /**
     * A parallel node started the steps of one entry.
     *
     * @param parallel the parallel node
     */
    default void entryFannedOut(NodeEvent parallel) {
    }

    /**
     * The content store adds a page document.
     *
     * @param content the topic
     * @return the observation, closed when the document is added or adding it fails
     */
    default Observation addingPage(ContentEvent content) {
        return Observation.NONE;
    }

    /**
     * The content store buffered the document of a page entry.
     *
     * @param content the topic
     */
    default void entryBuffered(ContentEvent content) {
    }

    /**
     * The content store buffered a document.
     *
     * @param content the topic
     */
    default void documentBuffered(ContentEvent content) {
    }

    /**
     * The content store publishes positions.
     *
     * @param content   the topic
     * @param positions the number of positions
     * @return the observation, closed when they are published or publishing fails
     */
    default Observation publishing(ContentEvent content, int positions) {
        return Observation.NONE;
    }
}
