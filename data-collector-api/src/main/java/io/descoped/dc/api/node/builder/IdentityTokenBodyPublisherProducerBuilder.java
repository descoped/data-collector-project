package io.descoped.dc.api.node.builder;

/**
 * Base for the builders of body publisher producers that put an identity token in the request body.
 */
public abstract class IdentityTokenBodyPublisherProducerBuilder extends BodyPublisherProducerBuilder {

    IdentityTokenBodyPublisherProducerBuilder(BuilderType type) {
        super(type);
    }


    static abstract class IdentityTokenNode extends LeafNode {

    }
}
