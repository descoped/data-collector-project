package io.descoped.dc.api.node;

/**
 * A predicate on a response.
 */
public interface ResponsePredicate extends Leaf {
    /**
     * The key of the execution state that holds the predicate's result.
     */
    String RESPONSE_PREDICATE_RESULT = "RESPONSE_PREDICATE_RESULT";
}
