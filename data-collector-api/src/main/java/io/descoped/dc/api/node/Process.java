package io.descoped.dc.api.node;

import io.descoped.dc.api.Processor;

import java.util.Set;

public interface Process extends Node {

    /**
     * The processor's class: the Java DSL's, or the one the named {@link io.descoped.dc.api.ProcessorProvider}
     * registers.
     *
     * @return the class
     * @throws IllegalStateException if no provider has the name
     */
    Class<? extends Processor> processorClass();

    /**
     * The name of the processor's provider, or the simple name of the Java DSL's class.
     *
     * @return the name
     */
    String processor();

    Set<String> requiredOutputs();

}
