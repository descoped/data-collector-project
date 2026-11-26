package io.descoped.dc.api.observer;

import java.util.UUID;

/**
 * A worker.
 *
 * @param workerId        the worker's id
 * @param specificationId the id of the specification it runs
 * @param name            the specification's name
 */
public record WorkerEvent(UUID workerId, String specificationId, String name) {
}
