package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.XmlToken;

/**
 * Builds an {@link XmlToken} query, which reads an XML response body as a stream of tokens instead of as one
 * document. The query has no expression of its own.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class XmlTokenBuilder extends QueryBuilder {

    /**
     * Creates the builder.
     */
    public XmlTokenBuilder() {
        super(BuilderType.QueryXmlToken);
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        return (R) new XmlTokenNode();
    }

    @Override
    public String toString() {
        return "XmlTokenBuilder{}";
    }

    static class XmlTokenNode extends QueryNode implements XmlToken {

        public XmlTokenNode() {
            // no state: the query returns the response body unaltered
        }

        // NOP expression - the body query returns unaltered response body
        @Override
        public String expression() {
            return Void.TYPE.getName();
        }

        @Override
        public String toString() {
            return "XmlTokenBuilder{}";
        }
    }
}
