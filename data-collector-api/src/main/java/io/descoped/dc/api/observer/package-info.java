/**
 * The engine observer: the service interface through which the engine reports what it does (workers, node
 * executions, requests and their retries, pagination, fan-out and the content store) to monitoring, such as
 * Prometheus metrics or, later, traces. Providers are found with {@link java.util.ServiceLoader}; neither a
 * specification nor a user of the engine registers them.
 */
package io.descoped.dc.api.observer;
