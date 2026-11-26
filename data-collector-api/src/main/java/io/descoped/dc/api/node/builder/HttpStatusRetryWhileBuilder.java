package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.http.HttpStatus;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.BodyContains;
import io.descoped.dc.api.node.HttpStatusRetryWhile;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Builds an {@link HttpStatusRetryWhile}: when an operation's response has the given status code, the engine waits
 * for the given time and sends the request again.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class HttpStatusRetryWhileBuilder extends LeafNodeBuilder {

    /**
     * The status code that makes the request be sent again.
     */
    @JsonProperty("statusCode")
    public Integer statusCode;
    /**
     * The unit of the time to wait before the request is sent again.
     */
    @JsonProperty("duration")
    public TimeUnit duration;
    /**
     * The time to wait before the request is sent again, in {@link #duration} units.
     */
    @JsonProperty("amount")
    public Integer amount;
    @JsonProperty("bodyContains")
    BodyContainsBuilder bodyContainsBuilder;

    /**
     * Creates the builder; set the status code and time with an {@code is} method.
     */
    public HttpStatusRetryWhileBuilder() {
        super(BuilderType.HttpStatusRetryWhile);
    }

    /**
     * Sets the status code to retry on and the time to wait.
     *
     * @param statusCode the status code
     * @param duration   the unit of the time to wait
     * @param amount     the time to wait, in {@code duration} units
     * @return this builder
     */
    public HttpStatusRetryWhileBuilder is(Integer statusCode, TimeUnit duration, Integer amount) {
        this.statusCode = statusCode;
        this.duration = duration;
        this.amount = amount;
        return this;
    }

    /**
     * Sets the status code to retry on and the time to wait.
     *
     * @param statusCode the status code
     * @param duration   the unit of the time to wait
     * @param amount     the time to wait, in {@code duration} units
     * @return this builder
     */
    public HttpStatusRetryWhileBuilder is(HttpStatus statusCode, TimeUnit duration, Integer amount) {
        this.statusCode = statusCode.code();
        this.duration = duration;
        this.amount = amount;
        return this;
    }

    // TODO: implement regex predicate support

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        BodyContains bodyContains = bodyContainsBuilder != null ? bodyContainsBuilder.build(buildContext) : null;
        return (R) new HttpStatusRetryWhileNode(statusCode, duration, amount, bodyContains);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HttpStatusRetryWhileBuilder that = (HttpStatusRetryWhileBuilder) o;
        return statusCode.equals(that.statusCode) &&
                duration == that.duration &&
                Objects.equals(amount, that.amount) &&
                Objects.equals(bodyContainsBuilder, that.bodyContainsBuilder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), statusCode, duration, amount, bodyContainsBuilder);
    }

    @Override
    public String toString() {
        return "HttpStatusRetryWhileBuilder{" +
                "statusCode=" + statusCode +
                ", duration=" + duration +
                ", amount=" + amount +
                ", bodyContainsBuilder=" + bodyContainsBuilder +
                '}';
    }

    static class HttpStatusRetryWhileNode extends LeafNode implements HttpStatusRetryWhile {

        final Integer statusCode;
        final TimeUnit duration;
        final Integer amount;
        final BodyContains bodyContains;

        HttpStatusRetryWhileNode(Integer statusCode, TimeUnit duration, Integer amount, BodyContains bodyContains) {
            this.statusCode = statusCode;
            this.duration = duration;
            this.amount = amount;
            this.bodyContains = bodyContains;
        }

        @Override
        public Integer statusCode() {
            return statusCode;
        }

        @Override
        public TimeUnit duration() {
            return duration;
        }

        @Override
        public Integer amount() {
            return amount;
        }

        @Override
        public BodyContains bodyContains() {
            return bodyContains;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HttpStatusRetryWhileNode that = (HttpStatusRetryWhileNode) o;
            return statusCode.equals(that.statusCode) &&
                    duration == that.duration &&
                    Objects.equals(amount, that.amount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statusCode, duration, amount);
        }

        @Override
        public String toString() {
            return "HttpStatusRetryWhileNode{" +
                    "statusCode=" + statusCode +
                    ", duration=" + duration +
                    ", amount=" + amount +
                    '}';
        }
    }

}
