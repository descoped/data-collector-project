package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Operation;

import java.util.Objects;

public abstract class OperationBuilder extends NodeWithIdBuilder {

    @JsonProperty
    String url;

    OperationBuilder(BuilderType type) {
        super(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OperationBuilder that = (OperationBuilder) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url);
    }

    abstract static class OperationNode extends FlowNodeWithId implements Operation {
        OperationNode(Configurations configurations, String id) {
            super(configurations, id);
        }
    }
}
