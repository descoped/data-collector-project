package io.descoped.dc.content.provider.discarding;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.service.provider.api.ProviderName;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates the {@code discarding} content store; it takes no configuration.
 */
@ProviderName("discarding")
public class DiscardingContentStoreInitializer implements ContentStoreInitializer {

    /**
     * Creates the initializer; the service loader calls it.
     */
    public DiscardingContentStoreInitializer() {
        // no state
    }

    @Override
    public Set<String> configurationKeys() {
        return new HashSet<>();
    }

    @Override
    public ContentStore initialize(Map<String, String> configuration) {
        return new DiscardingContentStore();
    }
}
