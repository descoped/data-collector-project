package io.descoped.dc.api.node.builder;

public abstract class IdentityTokenBodyPublisherProducerBuilder extends BodyPublisherProducerBuilder {

    IdentityTokenBodyPublisherProducerBuilder(BuilderType type) {
        super(type);
    }


    static abstract class IdentityTokenNode extends LeafNode {

    }
}
