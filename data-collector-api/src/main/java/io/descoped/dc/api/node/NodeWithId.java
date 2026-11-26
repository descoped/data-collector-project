package io.descoped.dc.api.node;

// identifiable nodes
/**
 * A node with an id, that an {@link Execute} node can target.
 */
public interface NodeWithId extends Node {

    /**
     * The node's id.
     *
     * @return the id
     */
    String id();

}
