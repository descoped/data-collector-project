package io.descoped.dc.api.node.builder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.descoped.dc.api.Processor;
import io.descoped.dc.api.ProcessorProvider;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.Process;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds a {@link Process} node, which runs a {@link Processor} on the current response and returns the variables it
 * outputs.
 */
@JsonDeserialize(using = NodeBuilderDeserializer.class)
public class ProcessBuilder extends NodeBuilder {

    // a class from the Java DSL, or the name of a ProcessorProvider; a specification only ever names a provider
    @JsonIgnore
    Class<? extends Processor> processorClass;
    @JsonIgnore
    String processor;
    @JsonProperty
    Set<String> requiredOutputs = new LinkedHashSet<>();

    /**
     * Creates the builder of a processor given by its class, for specifications built in Java.
     *
     * @param processorClass the processor's class
     * @throws NullPointerException if {@code processorClass} is {@code null}
     */
    public ProcessBuilder(Class<? extends Processor> processorClass) {
        super(BuilderType.Process);
        this.processorClass = Objects.requireNonNull(processorClass, "processorClass");
    }

    /**
     * Creates the builder of a processor registered under a name ({@link ProcessorProvider}).
     *
     * @param processor the provider's name
     * @throws NullPointerException if {@code processor} is {@code null}
     */
    public ProcessBuilder(String processor) {
        super(BuilderType.Process);
        this.processor = Objects.requireNonNull(processor, "processor");
    }

    /**
     * Adds a variable the processor is required to output.
     *
     * @param variable the variable's name
     * @return this builder
     */
    public ProcessBuilder output(String variable) {
        requiredOutputs.add(variable);
        return this;
    }

    /**
     * The provider's name, as a specification gives it.
     *
     * @return the name
     * @throws IllegalStateException if the builder has a class that no {@link ProcessorProvider} registers
     */
    @JsonProperty("processor")
    String processorName() {
        if (processor != null) {
            return processor;
        }
        return ProcessorProvider.nameOf(processorClass).orElseThrow(() -> new IllegalStateException(
                "A specification names a processor by its provider; no ProcessorProvider registers " + processorClass.getName()));
    }

    @SuppressWarnings("unchecked")
    @Override
    <R extends Base> R build(BuildContext buildContext) {
        return (R) new ProcessNode(buildContext.getInstance(SpecificationBuilder.GLOBAL_CONFIGURATION), processorClass, processor, requiredOutputs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ProcessBuilder that = (ProcessBuilder) o;
        return Objects.equals(processorClass, that.processorClass) &&
                Objects.equals(processor, that.processor) &&
                Objects.equals(requiredOutputs, that.requiredOutputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), processorClass, processor, requiredOutputs);
    }

    @Override
    public String toString() {
        return "ProcessBuilder{" +
                "processorClass=" + processorClass +
                ", processor=" + processor +
                ", requiredOutputs=" + requiredOutputs +
                '}';
    }

    static class ProcessNode extends FlowNode implements Process {

        final Class<? extends Processor> processorClass;
        final String processor;
        final Set<String> requiredOutputs;
        private final AtomicReference<Class<? extends Processor>> resolvedProcessorClass = new AtomicReference<>();

        ProcessNode(Configurations configurations, Class<? extends Processor> processorClass, String processor, Set<String> requiredOutputs) {
            super(configurations);
            this.processorClass = processorClass;
            this.processor = processor;
            this.requiredOutputs = requiredOutputs;
        }

        // a named processor is looked up when it first runs, so a specification can be read and built without its
        // providers; the class is kept, so the providers are not loaded again for every page and entry
        @Override
        public Class<? extends Processor> processorClass() {
            if (processorClass != null) {
                return processorClass;
            }
            Class<? extends Processor> resolved = resolvedProcessorClass.get();
            if (resolved == null) {
                resolved = ProcessorProvider.find(processor).orElseThrow(() ->
                        new IllegalStateException("No ProcessorProvider is named: " + processor));
                resolvedProcessorClass.compareAndSet(null, resolved);
            }
            return resolved;
        }

        @Override
        public String processor() {
            return processor != null ? processor : processorClass.getSimpleName();
        }

        @Override
        public Set<String> requiredOutputs() {
            return requiredOutputs;
        }

        @Override
        public Iterator<? extends Node> iterator() {
            return createNodeList().iterator();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessNode that = (ProcessNode) o;
            return Objects.equals(processorClass, that.processorClass) &&
                    Objects.equals(processor, that.processor) &&
                    Objects.equals(requiredOutputs, that.requiredOutputs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(processorClass, processor, requiredOutputs);
        }

        @Override
        public String toString() {
            return "ProcessNode{" +
                    "processorClass=" + processorClass +
                    ", processor=" + processor +
                    ", requiredOutputs=" + requiredOutputs +
                    '}';
        }
    }

}
