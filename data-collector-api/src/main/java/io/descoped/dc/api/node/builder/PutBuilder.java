package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.BodyPublisher;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Put;
import io.descoped.dc.api.node.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static io.descoped.dc.api.Builders.status;

/**
 * Builds a {@link Put} operation: an HTTP PUT request with headers and a body, response validators, and steps, the
 * pipes, that run on the response. Without a validator, the response must have status 200.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class PutBuilder extends OperationBuilder {

    @JsonUnwrapped(prefix = "request")
    Headers requestHeaders = new Headers();
    @JsonProperty("bodyPublisher")
    BodyPublisherBuilder bodyPublisherBuilder;
    @JsonProperty("responseValidators")
    List<LeafNodeBuilder> validators = new ArrayList<>();
    @JsonProperty("pipes")
    List<NodeBuilder> pipes = new ArrayList<>();
    @JsonProperty
    List<String> returnVariables = new ArrayList<>();

    PutBuilder() {
        super(BuilderType.Put);
    }

    /**
     * Creates the builder.
     *
     * @param id the operation's id, by which the specification refers to it as a function
     */
    public PutBuilder(String id) {
        super(BuilderType.Put);
        setId(id);
    }

    /**
     * Sets the operation's id.
     *
     * @param id the id
     * @return this builder
     */
    public PutBuilder id(String id) {
        setId(id);
        return this;
    }

    /**
     * Sets the request URL; it may contain expressions.
     *
     * @param urlString the URL
     * @return this builder
     */
    public PutBuilder url(String urlString) {
        this.url = urlString;
        return this;
    }

    /**
     * Adds a request header.
     *
     * @param name  the header's name
     * @param value the header's value
     * @return this builder
     */
    public PutBuilder header(String name, String value) {
        requestHeaders.put(name, value);
        return this;
    }

    /**
     * Sets the request body.
     *
     * @param bodyPublisherBuilder the body's builder
     * @return this builder
     */
    public PutBuilder data(BodyPublisherBuilder bodyPublisherBuilder) {
        this.bodyPublisherBuilder = bodyPublisherBuilder;
        return this;
    }

    /**
     * Adds a response validator, such as an {@link HttpStatusValidationBuilder}.
     *
     * @param validationBuilder the validator's builder
     * @return this builder
     */
    public PutBuilder validate(LeafNodeBuilder validationBuilder) {
        validators.add(validationBuilder);
        return this;
    }

    /**
     * Adds a step to run on the response.
     *
     * @param builder the step's builder
     * @return this builder
     */
    public PutBuilder pipe(NodeBuilder builder) {
        pipes.add(builder);
        return this;
    }

    /**
     * Adds names of variables the operation returns to its caller after its steps have run.
     *
     * @param variableKeys the variables' names
     * @return this builder
     */
    public PutBuilder returnVariables(String... variableKeys) {
        Collections.addAll(returnVariables, variableKeys);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        BodyPublisher bodyPublisher = bodyPublisherBuilder == null ? null : bodyPublisherBuilder.build(buildContext);

        List<Validator> validatorNodes = new ArrayList<>();

        // add default http status validator if unassigned
        if (this.validators.isEmpty()) {
            this.validate(status().success(200));
        }

        for (LeafNodeBuilder validatorBuilder : this.validators) {
            Validator validator = validatorBuilder.build(buildContext);
            validatorNodes.add(validator);
        }

        List<Node> stepNodeList = new ArrayList<>();
        for (NodeBuilder stepBuilder : pipes) {
            Node stepNode = stepBuilder.build(buildContext);
            stepNodeList.add(stepNode);
        }

        return (R) new PutBuilder.PutNode(getId(), buildContext.getInstance(SpecificationBuilder.GLOBAL_CONFIGURATION), url, requestHeaders, bodyPublisher, validatorNodes, stepNodeList, returnVariables);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PutBuilder that = (PutBuilder) o;
        return Objects.equals(requestHeaders, that.requestHeaders) &&
                Objects.equals(bodyPublisherBuilder, that.bodyPublisherBuilder) &&
                Objects.equals(validators, that.validators) &&
                Objects.equals(pipes, that.pipes) &&
                Objects.equals(returnVariables, that.returnVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requestHeaders, bodyPublisherBuilder, validators, pipes, returnVariables);
    }

    @Override
    public String toString() {
        return "PutBuilder{" +
                "requestHeaders=" + requestHeaders +
                ", bodyPublisherBuilder=" + bodyPublisherBuilder +
                ", validators=" + validators +
                ", pipes=" + pipes +
                ", returnVariables=" + returnVariables +
                '}';
    }

    static class PutNode extends OperationNode implements Put {

        final String url;
        final Headers headers;
        final List<Node> pipes;
        final List<String> returnVariables;
        final BodyPublisher bodyPublisher;
        final List<Validator> validateResponse;

        PutNode(String id, Configurations configurations, String url, Headers headers, BodyPublisher bodyPublisher, List<Validator> validateResponse, List<Node> pipes, List<String> returnVariables) {
            super(configurations, id);
            this.url = url;
            this.headers = headers;
            this.bodyPublisher = bodyPublisher;
            this.validateResponse = validateResponse;
            this.pipes = pipes;
            this.returnVariables = returnVariables;
        }

        @Override
        public String url() {
            return url;
        }

        @Override
        public Headers headers() {
            return headers;
        }

        @Override
        public BodyPublisher bodyPublisher() {
            return bodyPublisher;
        }

        @Override
        public List<Validator> responseValidators() {
            return validateResponse;
        }

        @Override
        public List<? extends Node> steps() {
            return pipes;
        }

        @Override
        public List<String> returnVariables() {
            return returnVariables;
        }

        @Override
        public Iterator<? extends Node> iterator() {
            return pipes.iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PutNode putNode = (PutNode) o;
            return Objects.equals(url, putNode.url) &&
                    Objects.equals(headers, putNode.headers) &&
                    Objects.equals(pipes, putNode.pipes) &&
                    Objects.equals(returnVariables, putNode.returnVariables) &&
                    Objects.equals(bodyPublisher, putNode.bodyPublisher) &&
                    Objects.equals(validateResponse, putNode.validateResponse);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, headers, pipes, returnVariables, bodyPublisher, validateResponse);
        }

        @Override
        public String toString() {
            return "PutNode{" +
                    "url='" + url + '\'' +
                    ", headers=" + headers +
                    ", pipes=" + pipes +
                    ", returnVariables=" + returnVariables +
                    ", bodyPublisher=" + bodyPublisher +
                    ", validateResponse=" + validateResponse +
                    '}';
        }
    }
}
