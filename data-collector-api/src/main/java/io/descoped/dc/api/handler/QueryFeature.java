package io.descoped.dc.api.handler;

import java.util.List;

/**
 * A query evaluated on a document or a value.
 */
public interface QueryFeature {

    /**
     * The kind of data a query is evaluated on or gives.
     */
    enum Type {
        /**
         * A list.
         */
        LIST,
        /**
         * An object.
         */
        OBJECT,
        /**
         * A string.
         */
        STRING_LITERAL;
    }

    /**
     * Evaluates the query to a list.
     *
     * @param data the document or value
     * @return the list
     */
    List<?> evaluateList(Object data);

    /**
     * Evaluates the query to an object.
     *
     * @param data the document or value
     * @return the object
     */
    Object evaluateObject(Object data);

    /**
     * Evaluates the query to a string.
     *
     * @param data the document or value
     * @return the string
     */
    String evaluateStringLiteral(Object data);

}
