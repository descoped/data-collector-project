package io.descoped.dc.api.node;

/**
 * A node that logs the current variables, response and body when it is executed.
 */
public interface Console extends Node {

    /**
     * Does nothing in the node; the engine's handler does the logging.
     */
    void log();

}
