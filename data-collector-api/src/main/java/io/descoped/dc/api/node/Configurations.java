package io.descoped.dc.api.node;

import io.descoped.dc.api.node.builder.BuildContext;
import io.descoped.dc.api.node.builder.SecurityBuilder;
import io.descoped.dc.api.node.builder.SpecificationContextBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The configurations of a specification, by configuration type.
 */
public class Configurations {

    final Map<Class<? extends Configuration>, Configuration> configurationMap;

    /**
     * Creates the configurations.
     *
     * @param configurationMap the configurations by type; kept, not copied
     */
    public Configurations(Map<Class<? extends Configuration>, Configuration> configurationMap) {
        this.configurationMap = configurationMap;
    }

    /**
     * The flow context.
     *
     * @return the flow context, or {@code null} if none is set
     */
    public FlowContext flowContext() {
        return (FlowContext) configurationMap.get(FlowContext.class);
    }

    /**
     * The security configuration.
     *
     * @return the security configuration, or {@code null} if none is set
     */
    public Security security() {
        return (Security) configurationMap.get(Security.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Configurations that = (Configurations) o;
        return Objects.equals(configurationMap, that.configurationMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configurationMap);
    }

    @Override
    public String toString() {
        return "Configurations{" +
                "configurationMap=" + configurationMap +
                '}';
    }

    /**
     * A builder of {@link Configurations}.
     */
    public static class Builder {
        final Map<Class<? extends Configuration>, Configuration> configurationMap = new LinkedHashMap<>();

        /**
         * Creates an empty builder.
         */
        public Builder() {
            // the state is in the fields
        }

        /**
         * Adds a configuration under its type, the first interface its class implements; it replaces one of the same
         * type.
         *
         * @param configuration the configuration
         * @return this builder
         */
        @SuppressWarnings("unchecked") // a configuration's first interface is its configuration type
        public Builder add(Configuration configuration) {
            Class<? extends Configuration> anInterface = (Class<? extends Configuration>) configuration.getClass().getInterfaces()[0];
            configurationMap.put(anInterface, configuration);
            return this;
        }

        /**
         * TODO: this is a negative side-state-effect induced by Worker.WorkerBuilder -> FlowContext. Counter measurers should not be necessary.
         */
        void createDefaultConfigurationIfAbsent() {
            if (!configurationMap.containsKey(FlowContext.class)) {
                SpecificationContextBuilder specificationContextBuilder = new SpecificationContextBuilder();
                if (specificationContextBuilder.globalState("global.topic") == null) {
                    specificationContextBuilder.globalState("global.topic", "topic");
                }
                configurationMap.put(FlowContext.class, specificationContextBuilder.build(BuildContext.empty()));
            }

            if (!configurationMap.containsKey(Security.class)) {
                SecurityBuilder securityBuilder = new SecurityBuilder();
                configurationMap.put(Security.class, securityBuilder.build(BuildContext.empty()));
            }
        }

        /**
         * Builds the configurations, adding a default flow context (topic {@code topic}) and a default security
         * configuration where none is set.
         *
         * @return the configurations
         */
        public Configurations build() {
            createDefaultConfigurationIfAbsent();
            return new Configurations(configurationMap);
        }
    }
}
