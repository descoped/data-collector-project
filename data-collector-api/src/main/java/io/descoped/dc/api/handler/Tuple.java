package io.descoped.dc.api.handler;

import java.util.Objects;

/**
 * A key and a value.
 *
 * @param <K> the key's type
 * @param <V> the value's type
 */
public class Tuple<K, V> {

    final K key;
    final V value;

    /**
     * Creates the tuple.
     *
     * @param key   the key
     * @param value the value
     */
    public Tuple(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * The key.
     *
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * The value.
     *
     * @return the value
     */
    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tuple)) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(key, tuple.key) &&
                Objects.equals(value, tuple.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "Tuple{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
