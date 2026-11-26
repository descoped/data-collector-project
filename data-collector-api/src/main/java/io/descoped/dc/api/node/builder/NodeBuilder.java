package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.util.JsonParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Base for the builders of flow nodes: the nodes of a specification's execution tree, which carry the global
 * configuration and can have children.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class NodeBuilder extends AbstractBuilder {

    NodeBuilder(BuilderType type) {
        super(type);
    }

    /**
     * Serializes this builder to pretty-printed JSON.
     *
     * @return the JSON
     */
    public String serialize() {
        return JsonParser.createJsonParser().toPrettyJSON(this);
    }

    /**
     * Serializes this builder to YAML.
     *
     * @return the YAML
     */
    public String serializeAsYaml() {
        return JsonParser.createYamlParser().toPrettyJSON(this);
    }

    /**
     * Successor is responsible for its own creation and must add itself to nodeInstanceById.
     * Lazy initialization is done through nodeBuilderById.
     *
     * @param buildContext @return
     */
    abstract <R extends Base> R build(BuildContext buildContext);

    // see FLowBuilder.end()
    /**
     * Builds the node with an empty {@link BuildContext} that holds a default (empty) global configuration.
     *
     * @param <R> the node type the caller expects
     * @return the node
     */
    public <R extends Base> R build() {
        BuildContext buildContext = BuildContext.empty();
        Configurations.Builder configurationsBuilder = new Configurations.Builder();
        Configurations configurations = configurationsBuilder.build();
        buildContext.cacheInstance(SpecificationBuilder.GLOBAL_CONFIGURATION, configurations);
        return build(buildContext);
    }

    /**
     * Base for flow node implementations: holds the global configuration and traverses the node tree depth first,
     * in pre-order.
     */
    public abstract static class FlowNode extends AbstractBaseNode implements Node {

        final Configurations configurations;

        /**
         * Creates the node; subclasses call it.
         *
         * @param configurations the global configuration of the specification
         */
        protected FlowNode(Configurations configurations) {
            this.configurations = configurations;
        }

        @Override
        public Configurations configurations() {
            return configurations;
        }

        static void depthFirstPreOrderFullTraversal(int depth, Set<Node> visitedNodeIds,
                                                    List<Node> ancestors, Node currentNode,
                                                    BiConsumer<List<Node>, Node> visit) {
            if (!visitedNodeIds.add(currentNode)) {
                return;
            }

            visit.accept(ancestors, currentNode);

            ancestors.add(currentNode);
            try {
                for (Iterator<? extends Node> it = currentNode.iterator(); it.hasNext(); ) {
                    depthFirstPreOrderFullTraversal(depth + 1, visitedNodeIds, ancestors, Optional.ofNullable(it.next()).orElseThrow(), visit);
                }
            } finally {
                ancestors.remove(currentNode);
            }
        }

        List<Node> createNodeList() {
            return new ArrayList<>();
        }

        @Override
        public void traverse(int depth, Set<Node> visitedNodeIds, List<Node> ancestors, BiConsumer<List<Node>, Node> visit) {
            depthFirstPreOrderFullTraversal(depth, visitedNodeIds, ancestors, this, visit);
        }

        @Override
        public String toPrintableExecutionPlan() {
            return PrintableExecutionPlan.build(this);
        }
    }
}
