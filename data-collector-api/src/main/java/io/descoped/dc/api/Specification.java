package io.descoped.dc.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.descoped.dc.api.node.Configurations;
import io.descoped.dc.api.node.Node;
import io.descoped.dc.api.node.builder.AbstractBuilder;
import io.descoped.dc.api.node.builder.BuildContext;
import io.descoped.dc.api.node.builder.NodeBuilderDeserializer;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.api.node.builder.SpecificationContextBuilder;
import io.descoped.dc.api.util.JsonParser;

import java.io.IOException;
import java.util.Map;

/**
 * A built specification: its id, name, configurations and the node it starts from.
 */
public class Specification {

    final String specificationId;
    final String name;
    final Configurations configurations;
    final Node startFunction;
    final Map<String, Node> nodeById;

    private Specification(String specificationId, String name, Node startFunction, Map<String, Node> nodeById) {
        this(specificationId, name, new Configurations.Builder().add(new SpecificationContextBuilder().build(BuildContext.empty())).build(), startFunction, nodeById);
    }

    private Specification(String specificationId, String name, Configurations configurations, Node startFunction, Map<String, Node> nodeById) {
        this.specificationId = specificationId;
        this.name = name;
        this.configurations = configurations;
        this.startFunction = startFunction;
        this.nodeById = nodeById;
    }

    /**
     * Starts a specification builder.
     *
     * @param specificationId the specification id
     * @param name            the name
     * @param startFunction   the id of the node the specification starts from
     * @return the builder
     */
    public static SpecificationBuilder start(String specificationId, String name, String startFunction) {
        return new SpecificationBuilder(specificationId, name, startFunction);
    }

    /**
     * Reads a specification builder from JSON or YAML.
     *
     * @param source the JSON or YAML
     * @return the builder
     * @throws RuntimeException if the source cannot be read
     */
    public static SpecificationBuilder deserialize(String source) {
        return deserialize(source, SpecificationBuilder.class);
    }

    /**
     * Reads a builder of a given type from JSON or YAML.
     *
     * @param source       the JSON or YAML
     * @param builderClass the builder type
     * @param <R>          the builder type
     * @return the builder
     * @throws RuntimeException if the source cannot be read
     */
    public static <R extends AbstractBuilder> R deserialize(String source, Class<R> builderClass) {
        try {
            ObjectMapper mapper = JsonParser.createYamlParser().mapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(AbstractBuilder.class, new NodeBuilderDeserializer());
            mapper.registerModule(module);
            return mapper.readValue(source, builderClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a specification with a default specification context.
     *
     * @param specificationId the specification id
     * @param name            the name
     * @param startFunction   the node the specification starts from
     * @param nodeById        the nodes by id
     * @return the specification
     */
    public static Specification create(String specificationId, String name, Node startFunction, Map<String, Node> nodeById) {
        return new Specification(specificationId, name, startFunction, nodeById);
    }

    /**
     * Creates a specification.
     *
     * @param specificationId the specification id
     * @param name            the name
     * @param configurations  the configurations
     * @param startFunction   the node the specification starts from
     * @param nodeById        the nodes by id
     * @return the specification
     */
    public static Specification create(String specificationId, String name, Configurations configurations, Node startFunction, Map<String, Node> nodeById) {
        return new Specification(specificationId, name, configurations, startFunction, nodeById);
    }

    /**
     * The specification id.
     *
     * @return the id
     */
    public String id() {
        return specificationId;
    }

    /**
     * The specification's name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * The node the specification starts from.
     *
     * @return the node
     */
    public Node startFunction() {
        return startFunction;
    }

}
