package io.descoped.dc.server.content;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.application.spi.Component;
import io.descoped.service.provider.api.ProviderConfigurator;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The content store the workers publish to, as a component of the application.
 */
public class ContentStoreComponent implements Component {

    private final DynamicConfiguration configuration;
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private ContentStore contentStore;

    /**
     * Creates the component; the store is created when it is initialized.
     *
     * @param configuration the configuration
     */
    public ContentStoreComponent(DynamicConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Creates the component.
     *
     * @param configuration the configuration
     * @return the component
     */
    public static ContentStoreComponent create(DynamicConfiguration configuration) {
        ContentStoreComponent contentStoreComponent = new ContentStoreComponent(configuration);
        contentStoreComponent.initialize();
        return contentStoreComponent;
    }

    @Override
    public void initialize() {
        if (isOpen()) {
            return;
        }
        contentStore = ProviderConfigurator.configure(ContentStoreInitializer.class, configuration.find("content.stream.connector").orElse(null), configuration.asMap());
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
    @SuppressWarnings("try") // ContentStore.close() is declared to throw Exception
    public void close() throws Exception {
        if (closed.get()) {
            return;
        }
        contentStore.close();
    }
}
