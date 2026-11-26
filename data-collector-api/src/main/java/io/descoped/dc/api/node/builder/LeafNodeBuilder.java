package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Leaf;
import io.descoped.dc.api.util.JsonParser;

/**
 * Base for the builders of leaf nodes: nodes without children, such as configuration, identities, request bodies
 * and response predicates.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class LeafNodeBuilder extends AbstractBuilder {

    LeafNodeBuilder(BuilderType type) {
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
     * Successor is responsible for its own creation and must add itself to nodeInstanceById.
     * Lazy initialization is done through nodeBuilderById.
     *
     * @param <R>          the node type the caller expects
     * @param buildContext the context of builders and node instances of this build
     * @return the node
     */
    public abstract <R extends Base> R build(BuildContext buildContext);

    /**
     * Builds the node with an empty {@link BuildContext}.
     *
     * @param <R> the node type the caller expects
     * @return the node
     */
    public <R extends Base> R build() {
        return build(BuildContext.empty());
    }

    abstract static class LeafNode extends AbstractBaseNode implements Leaf {
    }
}
