package io.descoped.dc.api.node;

import java.util.concurrent.TimeUnit;

/**
 * A rule that waits and retries a request while the response has a given status code.
 */
public interface HttpStatusRetryWhile extends Validator {

    /**
     * The status code that makes the request retry.
     *
     * @return the status code
     */
    Integer statusCode();

    /**
     * The unit of the wait.
     *
     * @return the unit
     */
    TimeUnit duration();

    /**
     * The length of the wait, in {@link #duration()} units.
     *
     * @return the amount
     */
    Integer amount();

    /**
     * The body predicate of the rule.
     *
     * @return the predicate, or {@code null} if none is set
     */
    BodyContains bodyContains();

}
