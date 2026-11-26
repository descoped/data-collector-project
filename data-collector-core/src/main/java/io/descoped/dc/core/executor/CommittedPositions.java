package io.descoped.dc.core.executor;

import io.descoped.dc.api.content.ContentStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The positions an earlier run of a worker's topic committed, so that a resumed run never writes them again (#71).
 * <p>
 * When the worker starts, it reads two marks from the content store: the page topic's last position (the last
 * complete page) and the topic's last position (the last committed entry).
 * <ul>
 * <li>A page at the last complete page's position is committed, and so is every entry of it.</li>
 * <li>In the first other page of the run, the entries up to and including the last committed entry, in the page's
 * order, are committed. If that entry is not in the page, none is.</li>
 * </ul>
 * After that page the entry mark is not used again, so a source that repeats a position later is not affected.
 * Positions are compared by equality and page order only: they need not be sortable. One worker uses an instance;
 * its pages are fetched in order.
 */
public final class CommittedPositions {

    private volatile String lastPage;
    private volatile String lastEntry;
    private final AtomicBoolean firstOtherPageSeen = new AtomicBoolean();
    // the committed positions of the pages seen, each taken once by the entry that would publish it
    private final Set<String> committed = ConcurrentHashMap.newKeySet();

    /**
     * Creates marks with nothing committed; {@link #read(ContentStore, String)} reads them.
     */
    public CommittedPositions() {
        // nothing committed until read
    }

    // marks given directly, for tests
    CommittedPositions(String lastPage, String lastEntry) {
        this.lastPage = lastPage;
        this.lastEntry = lastEntry;
    }

    /**
     * Reads the marks of a topic: the last position of its page topic and of the topic itself.
     *
     * @param contentStore the content store
     * @param topic        the topic, or {@code null} if the worker publishes to none
     */
    public void read(ContentStore contentStore, String topic) {
        if (topic == null || contentStore == null || contentStore.isClosed()) {
            return;
        }
        lastPage = contentStore.lastPosition(topic + "-pages");
        lastEntry = contentStore.lastPosition(topic);
        firstOtherPageSeen.set(false);
        committed.clear();
    }

    /**
     * Whether a page is the last complete page, which is never written again.
     *
     * @param pagePosition the page's position, or {@code null} if it has none
     * @return true if it is
     */
    public boolean isCommittedPage(Object pagePosition) {
        String page = lastPage;
        return pagePosition != null && page != null && page.equals(pagePosition.toString());
    }

    /**
     * The committed positions among a page's entries.
     *
     * @param pagePosition      the page's position, or {@code null} if it has none
     * @param expectedPositions the page's entry positions, in the page's order
     * @return the committed positions, empty if none
     */
    public Set<String> committedInPage(Object pagePosition, List<String> expectedPositions) {
        Set<String> inPage;
        if (isCommittedPage(pagePosition)) {
            inPage = new LinkedHashSet<>(expectedPositions);
        } else if (!firstOtherPageSeen.compareAndSet(false, true)) {
            inPage = Set.of();
        } else {
            String entry = lastEntry;
            int index = entry == null ? -1 : expectedPositions.indexOf(entry);
            inPage = index < 0 ? Set.of() : new LinkedHashSet<>(expectedPositions.subList(0, index + 1));
        }
        committed.addAll(inPage);
        return inPage;
    }

    /**
     * Takes a position that a page found committed: true once for it, so that it is completed without being written.
     *
     * @param position the position
     * @return true if it is committed
     */
    public boolean take(String position) {
        return committed.remove(position);
    }
}
