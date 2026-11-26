package io.descoped.dc.api.content;

/**
 * A page's document, kept until the page is complete: a page is committed to the page topic only when every entry of
 * it, and of every page before it, is published (#71).
 */
public final class PageDocument {

    private final String topic;
    private final String position;
    private final String contentKey;
    private final byte[] content;
    private final HttpRequestInfo httpRequestInfo;

    /**
     * Creates the document of a page. The content is kept as given, not copied.
     *
     * @param topic           the topic whose page topic the page is committed to
     * @param position        the page's position
     * @param contentKey      the content key
     * @param content         the page's content
     * @param httpRequestInfo the request that fetched the page
     */
    public PageDocument(String topic, String position, String contentKey, byte[] content, HttpRequestInfo httpRequestInfo) {
        this.topic = topic;
        this.position = position;
        this.contentKey = contentKey;
        this.content = content;
        this.httpRequestInfo = httpRequestInfo;
    }

    /**
     * Commits the page to the page topic of a content store.
     *
     * @param contentStore the content store
     */
    public void commit(ContentStore contentStore) {
        contentStore.addPaginationDocument(topic, position, contentKey, content, httpRequestInfo);
    }

    /**
     * The page's position.
     *
     * @return the position
     */
    public String position() {
        return position;
    }
}
