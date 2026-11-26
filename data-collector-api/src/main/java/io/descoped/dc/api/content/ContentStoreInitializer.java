package io.descoped.dc.api.content;

import io.descoped.service.provider.api.ProviderInitializer;

/**
 * The service interface of a content store provider: it creates a {@link ContentStore} from configuration.
 */
public interface ContentStoreInitializer extends ProviderInitializer<ContentStore> {
}
