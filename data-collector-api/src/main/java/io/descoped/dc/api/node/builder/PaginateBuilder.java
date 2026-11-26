package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Condition;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Execute;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Paginate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds a {@link Paginate} node, which pages through an API: for each page it runs its execute targets with its
 * variables, prefetches the next page when few positions are pending, and stops when its condition holds.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class PaginateBuilder extends NodeWithIdBuilder {

    @JsonProperty
    Map<String, String> variables = new LinkedHashMap<>();
    @JsonProperty("iterate")
    List<ExecuteBuilder> children = new ArrayList<>();
    @JsonProperty
    int threshold;
    @JsonProperty("until")
    ConditionBuilder conditionBuilder;
    @JsonProperty
    boolean addPageContent;
    @JsonProperty
    String positionVariable;

    PaginateBuilder() {
        super(BuilderType.Paginate);
    }

    /**
     * Creates the builder.
     *
     * @param id the paginate's id, by which the specification refers to it as a function
     */
    public PaginateBuilder(String id) {
        super(BuilderType.Paginate);
        setId(id);
    }

    /**
     * Sets a variable given to the targets, replacing an earlier one with the same name. The value applies only
     * when the input does not set the variable; an expression whose variable the input sets is evaluated.
     *
     * @param identifier the variable's name
     * @param expression the variable's value or expression
     * @return this builder
     */
    public PaginateBuilder variable(String identifier, String expression) {
        variables.put(identifier, expression);
        return this;
    }

    /**
     * Adds a target that runs for each page.
     *
     * @param executeBuilder the target's builder
     * @return this builder
     */
    public PaginateBuilder iterate(ExecuteBuilder executeBuilder) {
        children.add(executeBuilder);
        return this;
    }

    /**
     * Sets the prefetch threshold: the next page is fetched when fewer positions than this are pending.
     *
     * @param threshold the threshold
     * @return this builder
     */
    public PaginateBuilder prefetchThreshold(int threshold) {
        this.threshold = threshold;
        return this;
    }

    /**
     * Sets the condition that stops the pagination. It is required: building fails without it.
     *
     * @param conditionBuilder the condition's builder
     * @return this builder
     */
    public PaginateBuilder until(ConditionBuilder conditionBuilder) {
        this.conditionBuilder = conditionBuilder;
        return this;
    }

    /**
     * Makes each page's response body be added as content, under the position in the given variable.
     *
     * @param positionVariable the name of the variable that holds the page's position
     * @return this builder
     */
    public PaginateBuilder addPageContent(String positionVariable) {
        this.positionVariable = positionVariable;
        this.addPageContent = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        List<Execute> executeNodeList = new ArrayList<>();

        for (ExecuteBuilder executeBuilder : children) {
            Execute executeNode = executeBuilder.build(buildContext);
            executeNodeList.add(executeNode);
        }

        ConditionBuilder.ConditionNode conditionNode = conditionBuilder.build(buildContext);

        return (R) new PaginateNode(getId(), buildContext.getInstance(SpecificationBuilder.GLOBAL_CONFIGURATION), variables, addPageContent, positionVariable, executeNodeList, threshold, conditionNode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PaginateBuilder that = (PaginateBuilder) o;
        return threshold == that.threshold &&
                addPageContent == that.addPageContent &&
                variables.equals(that.variables) &&
                children.equals(that.children) &&
                Objects.equals(conditionBuilder, that.conditionBuilder) &&
                Objects.equals(positionVariable, that.positionVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variables, children, threshold, conditionBuilder, addPageContent, positionVariable);
    }

    @Override
    public String toString() {
        return "PaginateBuilder{" +
                "variables=" + variables +
                ", children=" + children +
                ", threshold=" + threshold +
                ", conditionBuilder=" + conditionBuilder +
                ", addPageContent=" + addPageContent +
                ", positionVariable='" + positionVariable + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    static class PaginateNode extends FlowNodeWithId implements Paginate {

        final Map<String, String> variables;
        final boolean addPageContent;
        final String positionVariable;
        final List<Execute> children;
        final int threshold;
        final ConditionBuilder.ConditionNode conditionNode;

        PaginateNode(String id, Configurations configurations, Map<String, String> variables, boolean addPageContent, String positionVariable, List<Execute> children, int threshold, ConditionBuilder.ConditionNode conditionNode) {
            super(configurations, id);
            this.variables = variables;
            this.addPageContent = addPageContent;
            this.positionVariable = positionVariable;
            this.children = children;
            this.threshold = threshold;
            this.conditionNode = conditionNode;
        }

        @Override
        public Set<String> variableNames() {
            return variables.keySet();
        }

        @Override
        public String variable(String name) {
            return variables.get(name);
        }

        @Override
        public boolean addPageContent() {
            return addPageContent;
        }

        @Override
        public String positionVariable() {
            return positionVariable;
        }

        @Override
        public List<Execute> targets() {
            return children;
        }

        @Override
        public int threshold() {
            return threshold;
        }

        @Override
        public Condition condition() {
            return conditionNode;
        }

        @Override
        public Iterator<? extends Node> iterator() {
            return children.iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaginateNode that = (PaginateNode) o;
            return addPageContent == that.addPageContent &&
                    threshold == that.threshold &&
                    variables.equals(that.variables) &&
                    Objects.equals(positionVariable, that.positionVariable) &&
                    children.equals(that.children) &&
                    Objects.equals(conditionNode, that.conditionNode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(variables, addPageContent, positionVariable, children, threshold, conditionNode);
        }

        @Override
        public String toString() {
            return "PaginateNode{" +
                    "variables=" + variables +
                    ", addPageContent=" + addPageContent +
                    ", positionVariable='" + positionVariable + '\'' +
                    ", children=" + children +
                    ", threshold=" + threshold +
                    ", conditionNode=" + conditionNode +
                    ", id='" + id + '\'' +
                    '}';
        }
    }
}
