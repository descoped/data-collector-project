package io.descoped.dc.server.recovery;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.application.spi.Component;
import io.descoped.service.provider.api.ProviderConfigurator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The content store a recovery writes to: messages are copied as they are, without encryption keys.
 */
@SuppressWarnings("try") // Component.close() is declared to throw Exception
public class RecoveryContentStoreComponent implements Component {

    private final DynamicConfiguration configuration;
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private ContentStore contentStore;

    /**
     * Creates the component; the store is created when it is initialized.
     *
     * @param configuration the configuration
     */
    public RecoveryContentStoreComponent(DynamicConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates the component.
     *
     * @param configuration the configuration
     * @return the component
     */
    public static RecoveryContentStoreComponent create(DynamicConfiguration configuration) {
        RecoveryContentStoreComponent contentStoreComponent = new RecoveryContentStoreComponent(configuration);
        contentStoreComponent.initialize();
        return contentStoreComponent;
    }

    @Override
    public void initialize() {
        if (isOpen()) {
            return;
        }
        Map<String, String> configMap = new LinkedHashMap<>(this.configuration.asMap());

        // the recovery store copies content that is already encrypted, so it is configured without encryption: every
        // rawdata.encryption.* key is removed (the primary store has no way to turn its own encryption off)
        configMap.keySet().removeIf(key -> key.startsWith("rawdata.encryption."));

        // custom rule to ensure that producer writes that to recovery folder
        String gscRecoveryFolder = configuration.find("local-temp-folder").orElse(null);
        if (gscRecoveryFolder != null) {
            configMap.put("local-temp-folder", gscRecoveryFolder + "-recovery");
        }
        contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, this.configuration.find("content.stream.connector").orElse(null), configMap);
        closed.set(false);
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    @Override
    @SuppressWarnings("unchecked") // the caller names the delegate's type
    public <R> R getDelegate() {
        Objects.requireNonNull(contentStore);
        return (R) contentStore;
    }

    @Override
    public void close() throws Exception {
        if (closed.get()) {
            return;
        }
        contentStore.close();
    }
}
