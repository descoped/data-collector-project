package io.descoped.dc.api.observer;

/**
 * The observation of an HTTP request.
 */
public interface RequestObservation extends Observation {

    /**
     * The observation that observes nothing.
     */
    RequestObservation NONE = new RequestObservation() {
    };

    /**
     * The request was answered; called before {@link #close()}.
     *
     * @param statusCode    the status code
     * @param emptyResponse whether the response has no body
     */
    default void responded(int statusCode, boolean emptyResponse) {
    }
}
