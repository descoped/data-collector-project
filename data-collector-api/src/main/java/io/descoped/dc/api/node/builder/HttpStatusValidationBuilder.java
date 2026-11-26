package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.HttpStatusValidation;
import io.descoped.dc.api.node.ResponsePredicate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an {@link HttpStatusValidation}, which validates an operation's response status code: a code listed as
 * success passes, if it has response predicates only when one of them holds; any other code fails the operation.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class HttpStatusValidationBuilder extends LeafNodeBuilder {

    @JsonProperty
    Map<Integer, List<ResponsePredicateBuilder>> success = new LinkedHashMap<>();
    @JsonProperty
    List<Integer> failed = new ArrayList<>();

    /**
     * Creates the builder without status codes.
     */
    public HttpStatusValidationBuilder() {
        super(BuilderType.HttpStatusValidation);
    }

    /**
     * Adds success status codes, replacing the response predicates of codes already added.
     *
     * @param statusCode the status codes
     * @return this builder
     */
    public HttpStatusValidationBuilder success(Integer... statusCode) {
        for (int sc : statusCode) {
            success.put(sc, new ArrayList<>());
        }
        return this;
    }

    /**
     * Adds the known status codes in a range as success codes, replacing the response predicates of codes already
     * added.
     *
     * @param fromStatusCodeInclusive the first status code of the range
     * @param toStatusCodeInclusive   the last status code of the range
     * @return this builder
     */
    public HttpStatusValidationBuilder success(Integer fromStatusCodeInclusive, Integer toStatusCodeInclusive) {
        List<Integer> statusCodes = HttpStatus.range(fromStatusCodeInclusive, toStatusCodeInclusive).stream().map(HttpStatus::code).toList();
        for (int sc : statusCodes) {
            success.put(sc, new ArrayList<>());
        }
        return this;
    }

    /**
     * Adds a success status code with a response predicate; the code passes only when one of its predicates holds.
     *
     * @param statusCode               the status code
     * @param responsePredicateBuilder the predicate's builder
     * @return this builder
     */
    public HttpStatusValidationBuilder success(Integer statusCode, ResponsePredicateBuilder responsePredicateBuilder) {
        success.computeIfAbsent(statusCode, list -> new ArrayList<>()).add(responsePredicateBuilder);
        return this;
    }

    /**
     * Adds failure status codes.
     *
     * @param statusCode the status codes
     * @return this builder
     */
    public HttpStatusValidationBuilder fail(Integer... statusCode) {
        failed.addAll(List.of(statusCode));
        return this;
    }

    /**
     * Adds the known status codes in a range as failure codes.
     *
     * @param fromStatusCodeInclusive the first status code of the range
     * @param toStatusCodeInclusive   the last status code of the range
     * @return this builder
     */
    public HttpStatusValidationBuilder fail(Integer fromStatusCodeInclusive, Integer toStatusCodeInclusive) {
        failed.addAll(HttpStatus.range(fromStatusCodeInclusive, toStatusCodeInclusive).stream().map(HttpStatus::code).toList());
        return this;
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        Map<HttpStatus, List<ResponsePredicate>> successMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<ResponsePredicateBuilder>> entry : success.entrySet()) {
            List<ResponsePredicate> responsePredicateList = entry.getValue().stream()
                    .map(builder -> (ResponsePredicate) builder.build(buildContext)).toList();
            successMap.computeIfAbsent(HttpStatus.valueOf(entry.getKey()), list -> new ArrayList<>()).addAll(responsePredicateList);
        }
        return (R) new HttpStatusValidationNode(successMap, failed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HttpStatusValidationBuilder that = (HttpStatusValidationBuilder) o;
        return Objects.equals(success, that.success) &&
                Objects.equals(failed, that.failed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), success, failed);
    }

    static class HttpStatusValidationNode extends LeafNode implements HttpStatusValidation {

        final Map<HttpStatus, List<ResponsePredicate>> success;
        final List<HttpStatus> failed;

        HttpStatusValidationNode(Map<HttpStatus, List<ResponsePredicate>> success, List<Integer> failed) {
            this.success = success;
            this.failed = failed.stream().map(HttpStatus::valueOf).toList();
        }

        @Override
        public Map<HttpStatus, List<ResponsePredicate>> success() {
            return success;
        }

        @Override
        public List<HttpStatus> failed() {
            return failed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HttpStatusValidationNode that = (HttpStatusValidationNode) o;
            return Objects.equals(success, that.success) &&
                    Objects.equals(failed, that.failed);
        }

        @Override
        public int hashCode() {
            return Objects.hash(success, failed);
        }

        @Override
        public String toString() {
            return "HttpStatusValidationNode{" +
                    "success=" + success +
                    ", failed=" + failed +
                    '}';
        }
    }
}
