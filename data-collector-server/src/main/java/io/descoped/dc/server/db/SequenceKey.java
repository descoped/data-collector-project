package io.descoped.dc.server.db;

import io.descoped.dc.api.ids.TimeOrderedIds;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A key of a topic's index: a message id and its position.
 */
public class SequenceKey implements Comparable<SequenceKey> {

    private final UUID id;
    private final String position;

    /**
     * Creates the key.
     *
     * @param id       the message id
     * @param position the position
     */
    public SequenceKey(UUID id, String position) {
        this.id = id;
        this.position = position;
    }

    /**
     * The message id.
     *
     * @return the id
     */
    public UUID id() {
        return id;
    }

    /**
     * The position.
     *
     * @return the position
     */
    public String position() {
        return position;
    }

    /**
     * Reads a key.
     *
     * @param keyBuffer the key's bytes
     * @return the key
     */
    public static SequenceKey fromByteBuffer(ByteBuffer keyBuffer) {
        Objects.requireNonNull(keyBuffer);
        int positionLength = keyBuffer.get();
        byte[] positionBytes = new byte[positionLength];
        keyBuffer.get(positionBytes);
        long idMostSignificantBits = keyBuffer.getLong();
        long idLeastSignificantBits = keyBuffer.getLong();
        String position = new String(positionBytes, UTF_8);
        UUID id = new UUID(idMostSignificantBits, idLeastSignificantBits);
        return new SequenceKey(id, position);
    }

    /**
     * Writes the key.
     *
     * @param allocatedBuffer the buffer to write to
     * @return the buffer, flipped
     */
    public ByteBuffer toByteBuffer(ByteBuffer allocatedBuffer) {
        Objects.requireNonNull(allocatedBuffer);
        byte[] key = position.getBytes(UTF_8);
        allocatedBuffer.put((byte) key.length);
        allocatedBuffer.put(key);
        allocatedBuffer.putLong(id.getMostSignificantBits());
        allocatedBuffer.putLong(id.getLeastSignificantBits());
        return allocatedBuffer.flip();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SequenceKey that = (SequenceKey) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position);
    }

    @Override
    public int compareTo(SequenceKey that) {
        return TimeOrderedIds.ORDER.compare(this.id, that.id);
    }

    @Override
    public String toString() {
        return "SequenceKey{" +
                "id=" + id +
                ", position='" + position + '\'' +
                '}';
    }
}
