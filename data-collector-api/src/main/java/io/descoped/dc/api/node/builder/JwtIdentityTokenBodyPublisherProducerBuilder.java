package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Identity;
import io.descoped.dc.api.node.JwtTokenBodyPublisherProducer;

import java.util.Objects;
import java.util.Optional;

import static io.descoped.dc.api.node.builder.SpecificationBuilder.GLOBAL_CONFIGURATION;

/**
 * Builds a {@link JwtTokenBodyPublisherProducer}, which gives a request body made from a signed JWT: the engine
 * creates the token from a JWT identity of the security configuration, binds it to a variable and evaluates the
 * token expression into the body.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class JwtIdentityTokenBodyPublisherProducerBuilder extends IdentityTokenBodyPublisherProducerBuilder {

    @JsonProperty
    String identityId;
    @JsonProperty
    String bindTo;
    @JsonProperty
    String token;

    /**
     * Creates the builder; set the identity, variable and token with the builder methods.
     */
    public JwtIdentityTokenBodyPublisherProducerBuilder() {
        super(BuilderType.JwtIdentityTokenBodyPublisherProducer);
    }

    /**
     * Sets the id of the identity in the security configuration; building fails if there is no such identity.
     *
     * @param identityId the identity's id
     * @return this builder
     */
    public JwtIdentityTokenBodyPublisherProducerBuilder identityId(String identityId) {
        this.identityId = identityId;
        return this;
    }

    /**
     * Sets the name of the variable the signed token is bound to.
     *
     * @param bindTo the variable's name
     * @return this builder
     */
    public JwtIdentityTokenBodyPublisherProducerBuilder bindTo(String bindTo) {
        this.bindTo = bindTo;
        return this;
    }

    /**
     * Sets the expression that gives the request body; it can use the variable the token is bound to.
     *
     * @param token the expression
     * @return this builder
     */
    public JwtIdentityTokenBodyPublisherProducerBuilder token(String token) {
        this.token = token;
        return this;
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        Configurations configurations = (Configurations) buildContext.nodeInstanceById().get(GLOBAL_CONFIGURATION);
        Optional<Identity> identity = Optional.ofNullable(configurations).stream().flatMap(conf -> conf.security().identities().stream().filter(i -> i.id().equals(identityId))).findFirst();
        if (identity.isEmpty()) {
            throw new IllegalStateException("Incorrect identity reference!");
        }
        return (R) new JwtTokenBodyPublisherProducerNode(identity.get(), bindTo, token);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JwtIdentityTokenBodyPublisherProducerBuilder builder = (JwtIdentityTokenBodyPublisherProducerBuilder) o;
        return Objects.equals(identityId, builder.identityId) &&
                Objects.equals(bindTo, builder.bindTo) &&
                Objects.equals(token, builder.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), identityId, bindTo, token);
    }

    @Override
    public String toString() {
        return "JwtIdentityTokenBodyPublisherProducerBuilder{" +
                "identityId='" + identityId + '\'' +
                ", bindTo='" + bindTo + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    static class JwtTokenBodyPublisherProducerNode extends LeafNode implements JwtTokenBodyPublisherProducer {

        private final Identity identity;
        private final String bindTo;
        private final String token;

        public JwtTokenBodyPublisherProducerNode(Identity identity, String bindTo, String token) {
            this.identity = identity;
            this.bindTo = bindTo;
            this.token = token;
        }

        @Override
        public Identity identity() {
            return identity;
        }

        @Override
        public String bindTo() {
            return bindTo;
        }

        @Override
        public String token() {
            return token;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JwtTokenBodyPublisherProducerNode that = (JwtTokenBodyPublisherProducerNode) o;
            return Objects.equals(identity, that.identity) &&
                    Objects.equals(bindTo, that.bindTo) &&
                    Objects.equals(token, that.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identity, bindTo, token);
        }

        @Override
        public String toString() {
            return "JwtTokenBodyPublisherProducerNode{" +
                    "identity=" + identity +
                    ", bindTo='" + bindTo + '\'' +
                    ", token='" + token + '\'' +
                    '}';
        }
    }
}
