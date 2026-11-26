package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.JsonToken;

/**
 * Builds a {@link JsonToken} query, which reads a JSON response body as a stream of tokens, one object at a time,
 * instead of as one document. The query has no expression of its own.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class JsonTokenBuilder extends QueryBuilder {

    /**
     * Creates the builder.
     */
    public JsonTokenBuilder() {
        super(BuilderType.QueryJsonToken);
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        return (R) new JsonTokenNode();
    }

    @Override
    public String toString() {
        return "JsonTokenBuilder{}";
    }

    static class JsonTokenNode extends QueryBuilder.QueryNode implements JsonToken {

        public JsonTokenNode() {
            // no state: the query returns the response body unaltered
        }

        // NOP expression - the body query returns unaltered response body
        @Override
        public String expression() {
            return Void.TYPE.getName();
        }

        @Override
        public String toString() {
            return "JsonTokenNode{}";
        }
    }
}
