package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Sequence;

import java.util.Iterator;
import java.util.Objects;

/**
 * Builds a {@link Sequence} node, which determines a page's expected positions: one query splits the page into
 * entries and another reads each entry's position.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class SequenceBuilder extends NodeBuilder {

    @JsonProperty("splitQuery")
    QueryBuilder splitBuilder;
    @JsonProperty("expectedQuery")
    QueryBuilder expectedBuilder;

    /**
     * Creates the builder; set the position query with {@link #expected(QueryBuilder)}.
     *
     * @param splitBuilder the query that splits the page into entries
     */
    public SequenceBuilder(QueryBuilder splitBuilder) {
        super(BuilderType.Sequence);
        this.splitBuilder = splitBuilder;
    }

    /**
     * Sets the query that splits the page into entries.
     *
     * @param splitBuilder the query
     */
    public void splitBuilder(QueryBuilder splitBuilder) {
        this.splitBuilder = splitBuilder;
    }

    /**
     * Sets the query that reads an entry's position. It is required: building fails without it.
     *
     * @param expectedBuilder the query
     * @return this builder
     */
    public SequenceBuilder expected(QueryBuilder expectedBuilder) {
        this.expectedBuilder = expectedBuilder;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        QueryBuilder.QueryNode splitToListQueryNode = splitBuilder.build(buildContext);
        QueryBuilder.QueryNode splitCriteriaQueryNode = expectedBuilder.build(buildContext);
        return (R) new SequenceNode(buildContext.getInstance(SpecificationBuilder.GLOBAL_CONFIGURATION), splitToListQueryNode, splitCriteriaQueryNode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SequenceBuilder that = (SequenceBuilder) o;
        return splitBuilder.equals(that.splitBuilder) &&
                expectedBuilder.equals(that.expectedBuilder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), splitBuilder, expectedBuilder);
    }

    @Override
    public String toString() {
        return "SequenceBuilder{" +
                "splitBuilder=" + splitBuilder +
                ", expectedBuilder=" + expectedBuilder +
                '}';
    }

    static class SequenceNode extends FlowNode implements Sequence {

        final QueryBuilder.QueryNode splitNode;
        final QueryBuilder.QueryNode expectedNode;

        SequenceNode(Configurations configurations, QueryBuilder.QueryNode splitNode, QueryBuilder.QueryNode expectedNode) {
            super(configurations);
            this.splitNode = splitNode;
            this.expectedNode = expectedNode;
        }

        @Override
        public QueryBuilder.QueryNode splitToListQuery() {
            return splitNode;
        }

        @Override
        public QueryBuilder.QueryNode expectedQuery() {
            return expectedNode;
        }

        @Override
        public Iterator<? extends Node> iterator() {
            return createNodeList().iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SequenceNode that = (SequenceNode) o;
            return splitNode.equals(that.splitNode) &&
                    expectedNode.equals(that.expectedNode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(splitNode, expectedNode);
        }

        @Override
        public String toString() {
            return "SequenceNode{" +
                    "splitNode=" + splitNode +
                    ", expectedNode=" + expectedNode +
                    '}';
        }
    }
}
