package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

    public abstract class IdentityNode extends LeafNode {

        protected final String id;

        protected IdentityNode(String id) {
            this.id = id;
        }
    }
}
