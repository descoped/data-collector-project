package io.descoped.dc.core.handler;

import java.util.Arrays;
import java.util.Objects;

/**
 * The current entry of a page: the entry as a parsed document node and its serialized content.
 */
public class PageEntryState {

    final Object nodeObject;
    final byte[] content;

    /**
     * Creates the state of an entry.
     *
     * @param nodeObject the entry as a parsed document node
     * @param content    the entry's serialized content
     */
    public PageEntryState(Object nodeObject, byte[] content) {
        this.nodeObject = nodeObject;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageEntryState that = (PageEntryState) o;
        return nodeObject.equals(that.nodeObject) &&
                Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(nodeObject);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

}
