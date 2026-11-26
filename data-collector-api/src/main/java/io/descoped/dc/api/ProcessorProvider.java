package io.descoped.dc.api;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A {@link Processor} registered under a name, so that a specification can run it: a {@code Process} node names the
 * provider ({@code "processor": "<name>"} in JSON or YAML, {@code Builders.process("<name>")} in Java), never a class,
 * and no class is loaded by a name from a specification.
 *
 * <p>A provider is a service: it is registered with {@code provides} in its {@code module-info.java} and in
 * {@code META-INF/services}, and it has a public constructor without parameters. The processor is created for each
 * execution, as with {@code Builders.process(Class)}.
 */
public interface ProcessorProvider {

    /**
     * The name a specification uses.
     *
     * @return the name
     */
    String name();

    /**
     * The processor the name stands for.
     *
     * @return the processor's class
     */
    Class<? extends Processor> processorClass();

    /**
     * The processor registered under a name.
     *
     * @param name the name
     * @return the processor's class, or empty when no provider has the name
     * @throws IllegalStateException if more than one provider has the name, so that a second jar cannot replace a
     *                               processor unnoticed
     */
    static Optional<Class<? extends Processor>> find(String name) {
        Objects.requireNonNull(name, "name");
        List<Class<? extends Processor>> named = ServiceLoader.load(ProcessorProvider.class, ProcessorProvider.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> name.equals(provider.name()))
                .<Class<? extends Processor>>map(ProcessorProvider::processorClass)
                .toList();
        if (named.size() > 1) {
            throw new IllegalStateException("More than one ProcessorProvider is named: " + name);
        }
        return named.stream().findFirst();
    }

    /**
     * The name a processor is registered under.
     *
     * @param processorClass the processor's class
     * @return the name, or empty when no provider has the class
     */
    static Optional<String> nameOf(Class<? extends Processor> processorClass) {
        Objects.requireNonNull(processorClass, "processorClass");
        return ServiceLoader.load(ProcessorProvider.class, ProcessorProvider.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> processorClass.equals(provider.processorClass()))
                .map(ProcessorProvider::name)
                .findFirst();
    }
}
