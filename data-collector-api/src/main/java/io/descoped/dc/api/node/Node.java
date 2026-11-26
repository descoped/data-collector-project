package io.descoped.dc.api.node;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

// implements FlowNode and step-nodes
/**
 * A node of the specification's flow, with its configurations and children.
 */
public interface Node extends Base {

    /**
     * The specification's configurations.
     *
     * @return the configurations
     */
    Configurations configurations();

    /**
     * The node's children.
     *
     * @return an iterator over the children
     */
    Iterator<? extends Node> iterator();

    /**
     * Visits this node and its descendants depth first, in pre-order, each node once.
     *
     * @param depth          the depth of this node
     * @param visitedNodeIds the nodes visited so far; this node is skipped if it is in the set
     * @param ancestors      the ancestors of this node, from the root
     * @param visit          called with the ancestors and the node for each node
     */
    void traverse(int depth, Set<Node> visitedNodeIds, List<Node> ancestors, BiConsumer<List<Node>, Node> visit);

    /**
     * A text rendering of the execution plan from this node.
     *
     * @return the plan
     */
    String toPrintableExecutionPlan();

}
