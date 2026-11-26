package io.descoped.dc.core.handler;

import io.descoped.dc.api.node.Base;

/**
 * The base of the handlers of response predicates, which test the response of a request.
 *
 * @param <N> the type of predicate node the handler executes
 */
public abstract class AbstractResponsePredicateHandler<N extends Base> extends AbstractHandler<N> {

    /**
     * Creates a handler for a response predicate.
     *
     * @param node the predicate node to execute
     */
    protected AbstractResponsePredicateHandler(N node) {
        super(node);
    }

}
