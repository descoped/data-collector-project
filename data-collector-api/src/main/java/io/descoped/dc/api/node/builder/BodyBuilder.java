package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Body;

/**
 * Builds a {@link Body} query, which returns the response body unaltered.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class BodyBuilder extends QueryBuilder {

    /**
     * Creates the builder.
     */
    public BodyBuilder() {
        super(BuilderType.QueryBody);
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        return (R) new BodyNode();
    }

    @Override
    public String toString() {
        return "BodyBuilder{}";
    }

    class BodyNode extends QueryNode implements Body {

        public BodyNode() {
            // no state: the query returns the response body unaltered
        }

        // NOP expression - the body query returns unaltered response body
        @Override
        public String expression() {
            return Void.TYPE.getName();
        }

        @Override
        public String toString() {
            return "BodyNode{}";
        }
    }

}
