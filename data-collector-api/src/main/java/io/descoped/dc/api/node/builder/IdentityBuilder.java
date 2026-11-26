package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Base for the builders of identities in the security configuration; each identity has an id.
 */
public abstract class IdentityBuilder extends LeafNodeBuilder {

    @JsonProperty
    String id;

    IdentityBuilder(BuilderType type, String id) {
        super(type);
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IdentityBuilder that = (IdentityBuilder) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    /**
     * Base for identity node implementations.
     */
    public abstract class IdentityNode extends LeafNode {

        /**
         * The identity's id.
         */
        protected final String id;

        /**
         * Creates the node; subclasses call it.
         *
         * @param id the identity's id
         */
        protected IdentityNode(String id) {
            this.id = id;
        }
    }
}
