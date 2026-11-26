package io.descoped.dc.server.recovery;

import io.descoped.dc.api.ids.TimeOrderedIds;

import java.util.Objects;
import java.util.UUID;

/**
 * Maintain the state of the oldest version of an id
 */
public class PositionAndIdVersion {

    private String position;
    private UUID id;

    /**
     * Creates an empty entry.
     */
    public PositionAndIdVersion() {
        // position and id are set by SequenceDbHelper
    }

    /**
     * The position.
     *
     * @return the position, or {@code null}
     */
    public String position() {
        return position;
    }

    /**
     * The message id.
     *
     * @return the id, or {@code null}
     */
    public UUID id() {
        return id;
    }

    /**
     * Whether no position is kept.
     *
     * @return true if none is
     */
    public boolean isEmpty() {
        return this.id == null && this.position == null;
    }

    /**
     * Whether an id is older than the one kept, or none is kept.
     *
     * @param id the id
     * @return true if it is
     */
    public boolean isOlder(UUID id) {
        if (isEmpty()) {
            return true; // ensure we accept
        }
        return TimeOrderedIds.ORDER.compare(this.id, id) > 0;
    }

    /**
     * Keeps an id and a position if the id is older than the one kept.
     *
     * @param id       the id
     * @param position the position
     * @return true if they were kept
     */
    public boolean compareAndSet(UUID id, String position) {
        if (isOlder(id)) {
            this.id = id;
            this.position = position;
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PositionAndIdVersion that = (PositionAndIdVersion) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position);
    }

    @Override
    public String toString() {
        return "PositionAndIdVersion{" +
                "id=" + id +
                ", position='" + position + '\'' +
                '}';
    }
}
