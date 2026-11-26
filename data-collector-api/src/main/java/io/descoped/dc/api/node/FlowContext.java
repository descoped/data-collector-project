package io.descoped.dc.api.node;

import io.descoped.dc.api.context.ExecutionContext;

/**
 * The specification's context: the topic and the global execution context.
 */
public interface FlowContext extends Configuration {

    /**
     * The name of the topic the content is published to.
     *
     * @return the topic name
     */
    String topic();

    /**
     * The global execution context: the global state and variables of the specification.
     *
     * @return the context
     */
    ExecutionContext globalContext();

}
