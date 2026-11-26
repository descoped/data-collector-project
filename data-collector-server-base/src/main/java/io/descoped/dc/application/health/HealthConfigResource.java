package io.descoped.dc.application.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.util.JsonParser;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The configuration in the health output, with the values of secret-looking keys masked.
 */
@HealthRenderPriority(priority = 30)
public class HealthConfigResource implements HealthResource {

    /**
     * Creates the resource; it reports no configuration until one is set.
     */
    public HealthConfigResource() {
        // the state is in the fields
    }

    private final AtomicReference<ObjectNode> configNode = new AtomicReference<>(JsonParser.createJsonParser().createObjectNode());

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "configuration";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return Set.of("config", "all").stream().anyMatch(queryParams::containsKey);
    }

    @Override
    public Object resource() {
        return configNode.get();
    }

    /**
     * Sets the configuration to report.
     *
     * @param configuration the configuration
     */
    public void setConfiguration(Map<String, String> configuration) {
        // configuration values never reach health output: every value is masked, the keys stay
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : configuration.keySet()) {
            map.put(key, "************");
        }
        JsonParser jsonParser = JsonParser.createJsonParser();
        configNode.set(jsonParser.mapper().convertValue(map, ObjectNode.class));
    }
}
