/**
 * The content store provider {@code discarding}, the engine's default: it keeps the last published position in
 * memory and drops the content.
 */
module io.descoped.dc.content.discarding {

    requires io.descoped.service.provider.api;
    requires transitive io.descoped.dc.api;

    requires org.slf4j;

    provides io.descoped.dc.api.content.ContentStoreInitializer with io.descoped.dc.content.provider.discarding.DiscardingContentStoreInitializer;

    exports io.descoped.dc.content.provider.discarding;
}
