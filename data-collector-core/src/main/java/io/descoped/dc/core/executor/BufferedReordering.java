package io.descoped.dc.core.executor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Releases completed elements in the order they were expected: an element is released once it and every element
 * expected before it have completed. Adding elements is thread safe.
 *
 * @param <ELEMENT> the element type
 */
public class BufferedReordering<ELEMENT> {

    private final Object lock = new Object();
    private final List<ELEMENT> expected = new ArrayList<>();
    private final Set<ELEMENT> completed = new LinkedHashSet<>();

    /**
     * Creates an empty reordering.
     */
    public BufferedReordering() {
        // nothing expected or completed until added
    }

    /**
     * The expected elements not yet released, in order. The live list, not a copy, and not synchronized.
     *
     * @return the expected elements
     */
    public List<ELEMENT> expected() {
        return expected;
    }

    /**
     * The completed elements not yet released. The live set, not a copy, and not synchronized.
     *
     * @return the completed elements
     */
    public Set<ELEMENT> completed() {
        return completed;
    }

    /**
     * Adds an expected element after those already expected.
     *
     * @param element the element
     */
    public void addExpected(ELEMENT element) {
        synchronized (lock) {
            expected.add(element);
        }
    }

    /**
     * Adds expected elements, in order, after those already expected.
     *
     * @param elements the elements
     */
    public void addExpected(List<ELEMENT> elements) {
        synchronized (lock) {
            expected.addAll(elements);
        }
    }

    /**
     * Marks an element completed and releases, in expected order, every completed element at the head of the expected
     * elements. The callback is called only when elements are released, and under the lock: two threads that release
     * elements one after the other hand them on in that order, whether or not the callback locks anything itself. So the
     * callback must not wait for another thread that adds expected or completed elements to this instance; it may start
     * such work asynchronously. While it runs, other threads that add elements wait.
     *
     * @param elements                the completed element
     * @param orderedElementsCallback receives the released elements, in order
     */
    public void addCompleted(ELEMENT elements, Consumer<List<ELEMENT>> orderedElementsCallback) {
        // the callback runs under the lock: released outside it, a later release could be handed on before an earlier
        // one, and a store that does not lock its topics (discarding) would publish positions out of order (#69)
        synchronized (lock) {
            completed.add(elements);
            List<ELEMENT> orderedElements = new ArrayList<>();
            Iterator<ELEMENT> iterator = expected.iterator();
            while (iterator.hasNext()) {
                ELEMENT element = iterator.next();
                if (!completed.remove(element)) {
                    break;
                }
                iterator.remove();
                orderedElements.add(element);
            }
            if (!orderedElements.isEmpty()) {
                orderedElementsCallback.accept(orderedElements);
            }
        }
    }
}
