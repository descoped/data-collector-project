package io.descoped.dc.api.observer;

/**
 * A node of a specification.
 *
 * @param type the node's type, the simple name of its interface, such as {@code Get} or {@code Paginate}
 * @param id   the node's id, or {@code null} for a node without one
 */
public record NodeEvent(String type, String id) {
}
