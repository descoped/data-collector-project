package io.descoped.dc.api.handler;

import java.util.Objects;

/**
 * The data a query is evaluated on, with its kind, kept in the execution state.
 *
 * @param <T> the data's type
 */
public class QueryState<T> {

    private final QueryFeature.Type type;
    private final T data;

    /**
     * Creates the state.
     *
     * @param type the kind of data
     * @param data the data
     */
    public QueryState(QueryFeature.Type type, T data) {
        this.type = type;
        this.data = data;
    }

    /**
     * The kind of data.
     *
     * @return the kind
     */
    public QueryFeature.Type type() {
        return type;
    }

    /**
     * The data.
     *
     * @return the data
     */
    public T data() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryState<?> that = (QueryState<?>) o;
        return type == that.type &&
                data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }

    @Override
    public String toString() {
        return "QueryState{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}
