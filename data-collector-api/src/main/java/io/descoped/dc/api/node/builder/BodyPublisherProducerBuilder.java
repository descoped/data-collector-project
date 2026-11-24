package io.descoped.dc.api.node.builder;

public abstract class BodyPublisherProducerBuilder extends LeafNodeBuilder {

    BodyPublisherProducerBuilder(BuilderType type) {
        super(type);
    }

    abstract static class BodyPublisherProducerNode extends LeafNode {
    }

}
