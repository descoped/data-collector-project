package io.descoped.dc.api.node.builder;

/**
 * Base for the builders of the producers that give a {@code BodyPublisher} its content.
 */
public abstract class BodyPublisherProducerBuilder extends LeafNodeBuilder {

    BodyPublisherProducerBuilder(BuilderType type) {
        super(type);
    }

    abstract static class BodyPublisherProducerNode extends LeafNode {
    }

}
