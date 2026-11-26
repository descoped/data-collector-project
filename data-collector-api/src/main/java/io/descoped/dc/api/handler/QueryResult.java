package io.descoped.dc.api.handler;

import java.util.Objects;

/**
 * The result of a query, kept in the execution state.
 *
 * @param <T> the result's type
 */
public class QueryResult<T> {

    private final T result;

    /**
     * Creates the result.
     *
     * @param result the result
     */
    public QueryResult(T result) {
        this.result = result;
    }

    /**
     * The result.
     *
     * @return the result
     */
    public T getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResult<?> that = (QueryResult<?>) o;
        return Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result);
    }

    @Override
    public String toString() {
        return "QueryResult{" +
                "result=" + result +
                '}';
    }
}
