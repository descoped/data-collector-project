package io.descoped.dc.core.content;

import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.HealthContentStreamMonitor;
import io.descoped.dc.api.content.HttpRequestInfo;
import io.descoped.dc.api.observer.ContentEvent;
import io.descoped.dc.api.observer.EngineObserver;
import io.descoped.dc.api.observer.Observation;
import io.descoped.dc.core.observer.EngineObservers;

import java.util.Map;
import java.util.Set;

/**
 * A content store that reports its writes to the engine observers: pages added, entries and documents buffered, and
 * positions published.
 */
public final class ObservedContentStore implements ContentStore {

    private final ContentStore delegate;
    private final EngineObserver observer;

    ObservedContentStore(ContentStore delegate, EngineObserver observer) {
        this.delegate = delegate;
        this.observer = observer;
    }

    /**
     * Observes a content store with the engine observers.
     *
     * @param contentStore the content store
     * @return the observed content store, or the content store itself when it is observed already
     */
    public static ContentStore of(ContentStore contentStore) {
        return contentStore instanceof ObservedContentStore ? contentStore : new ObservedContentStore(contentStore, EngineObservers.instance());
    }

    @Override
    public void lock(String topic) {
        delegate.lock(topic);
    }

    @Override
    public void unlock(String topic) {
        delegate.unlock(topic);
    }

    @Override
    public ContentStream contentStream() {
        return delegate.contentStream();
    }

    @Override
    public String lastPosition(String topic) {
        return delegate.lastPosition(topic);
    }

    @Override
    public Set<String> contentKeys(String topic, String position) {
        return delegate.contentKeys(topic, position);
    }

    @Override
    public void addPaginationDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
        Observation observation = observer.addingPage(new ContentEvent(topic));
        try {
            delegate.addPaginationDocument(topic, position, contentKey, content, httpRequestInfo);
        } catch (RuntimeException | Error e) {
            observation.failed(e);
            throw e;
        } finally {
            observation.close();
        }
    }

    @Override
    public void bufferPaginationEntryDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo, Map<String, Object> state) {
        delegate.bufferPaginationEntryDocument(topic, position, contentKey, content, httpRequestInfo, state);
        observer.entryBuffered(new ContentEvent(topic));
    }

    @Override
    public void bufferDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
        delegate.bufferDocument(topic, position, contentKey, content, httpRequestInfo);
        observer.documentBuffered(new ContentEvent(topic));
    }

    @Override
    public void publish(String topic, String... position) {
        Observation observation = observer.publishing(new ContentEvent(topic), position == null ? 0 : position.length);
        try {
            delegate.publish(topic, position);
        } catch (RuntimeException | Error e) {
            observation.failed(e);
            throw e;
        } finally {
            observation.close();
        }
    }

    @Override
    public HealthContentStreamMonitor monitor() {
        return delegate.monitor();
    }

    @Override
    public void closeTopic(String topic) {
        delegate.closeTopic(topic);
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
