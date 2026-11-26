package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Identity;
import io.descoped.dc.api.node.Security;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds the {@link Security} configuration of a specification: the SSL bundle of its client certificate and its
 * identities.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class SecurityBuilder extends ConfigurationBuilder {

    @JsonProperty
    String bundleName;
    @JsonProperty("identities")
    List<IdentityBuilder> identities = new ArrayList<>();

    /**
     * Creates the builder without an SSL bundle or identities.
     */
    public SecurityBuilder() {
        super(BuilderType.Security);
    }

    /**
     * Sets the name of the SSL bundle the HTTP client uses.
     *
     * @param bundleName the SSL bundle's name
     * @return this builder
     */
    public SecurityBuilder sslBundleName(String bundleName) {
        this.bundleName = bundleName;
        return this;
    }

    /**
     * Adds an identity.
     *
     * @param identityBuilder the identity's builder
     * @return this builder
     */
    public SecurityBuilder identity(IdentityBuilder identityBuilder) {
        this.identities.add(identityBuilder);
        return this;
    }

    @SuppressWarnings("unchecked") // the caller names the node type it builds
    @Override
    public <R extends Base> R build(BuildContext buildContext) {
        List<Identity> identityList = new ArrayList<>();
        for (IdentityBuilder identityBuilder : identities) {
            identityList.add(identityBuilder.build(buildContext));
        }
        return (R) new SecurityNode(bundleName, identityList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SecurityBuilder that = (SecurityBuilder) o;
        return bundleName.equals(that.bundleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bundleName);
    }

    @Override
    public String toString() {
        return "SecurityBuilder{" +
                "bundleName='" + bundleName + '\'' +
                '}';
    }

    /**
     * The {@link Security} configuration this builder builds.
     */
    public static class SecurityNode extends LeafNode implements Security {

        private final String sslBundleName;
        private final List<Identity> identities;

        /**
         * Creates the node.
         *
         * @param sslBundleName the name of the SSL bundle, or {@code null} for none
         * @param identities    the identities
         */
        public SecurityNode(String sslBundleName, List<Identity> identities) {
            this.sslBundleName = sslBundleName;
            this.identities = identities;
        }

        @Override
        public String sslBundleName() {
            return sslBundleName;
        }

        @Override
        public List<Identity> identities() {
            return identities;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SecurityNode that = (SecurityNode) o;
            return Objects.equals(sslBundleName, that.sslBundleName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sslBundleName);
        }

        @Override
        public String toString() {
            return "SecurityNode{" +
                    "sslBundleName='" + sslBundleName + '\'' +
                    '}';
        }
    }
}
