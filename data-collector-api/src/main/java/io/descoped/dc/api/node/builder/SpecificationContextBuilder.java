package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.ConfigSetter;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.FlowContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a specification's {@link FlowContext}: the request headers, variables and global state every function of
 * the specification starts with, among them the topic the content is published to.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class SpecificationContextBuilder extends ConfigurationBuilder {

    Headers headers = new Headers();
    @JsonProperty
    Map<String, Object> variables = new LinkedHashMap<>();
    @JsonProperty
    Map<Object, Object> globalState = new LinkedHashMap<>();

    /**
     * Creates the builder without headers, variables or global state.
     */
    public SpecificationContextBuilder() {
        super(BuilderType.SpecificationContext);
    }

    /**
     * The request headers, serialized as the {@code headers} property.
     *
     * @return the headers by name
     */
    @JsonProperty("headers")
    public Map<String, List<String>> headerMap() {
        return headers.asMap();
    }

    /**
     * Sets the topic the content is published to, as the global state {@code global.topic}.
     *
     * @param topicName the topic's name
     * @return this builder
     */
    @ConfigSetter
    public SpecificationContextBuilder topic(String topicName) {
        globalState("global.topic", topicName);
        return this;
    }

    /**
     * Adds a request header that every request of the specification sends.
     *
     * @param name  the header's name
     * @param value the header's value
     * @return this builder
     */
    @ConfigSetter
    public SpecificationContextBuilder header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    /**
     * Sets a variable, replacing an earlier one with the same name.
     *
     * @param name  the variable's name
     * @param value the variable's value
     * @return this builder
     */
    @ConfigSetter
    public SpecificationContextBuilder variable(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    /**
     * Sets a global state entry, replacing an earlier one with the same key.
     *
     * @param key   the entry's key
     * @param value the entry's value
     * @return this builder
     */
    @ConfigSetter
    public SpecificationContextBuilder globalState(Object key, Object value) {
        globalState.put(key, value);
        return this;
    }

    /**
     * A global state entry.
     *
     * @param key the entry's key
     * @return the entry's value, or {@code null} if there is none
     */
    public Object globalState(Object key) {
        return globalState.get(key);
    }

    /*
     * TODO the ExecutionContext.join() and overlays of context state must be improved. Possible race conditions here.
     * see FlowBuilder.end() and NodeBuilder.build().
     */
    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        ExecutionContext context = ExecutionContext.empty();
        // do not set set if headers is empty
        if (!headers.asMap().isEmpty()) {
            context.state(Headers.class, headers);
        }
        context.variables().putAll(variables);
        globalState.forEach(context::globalState);
        return (R) new FlowContextNode(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SpecificationContextBuilder that = (SpecificationContextBuilder) o;
        return Objects.equals(headers, that.headers) &&
                Objects.equals(variables, that.variables) &&
                Objects.equals(globalState, that.globalState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), headers, variables, globalState);
    }

    @Override
    public String toString() {
        return "SpecificationContextBuilder{" +
                "headers=" + headers +
                ", variables=" + variables +
                ", globalState=" + globalState +
                '}';
    }

    static class FlowContextNode extends LeafNode implements FlowContext {

        private final ExecutionContext context;

        FlowContextNode(ExecutionContext context) {
            this.context = context;
        }

        public String topic() {
            String topic = context.state("global.topic");
            if (topic == null) {
                throw new IllegalStateException("Topic is NOT configured!");
            }
            return topic;
        }

        @Override
        public ExecutionContext globalContext() {
            return context;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FlowContextNode that = (FlowContextNode) o;
            return Objects.equals(context, that.context);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context);
        }

        @Override
        public String toString() {
            return "FlowContextNode{" +
                    "context=" + context +
                    '}';
        }
    }
}
