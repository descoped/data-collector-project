package io.descoped.dc.api.node;

import java.util.List;

/**
 * An HTTP GET operation.
 */
public interface Get extends Operation {

    /**
     * The rules that retry the request on a status code.
     *
     * @return the rules
     */
    List<HttpStatusRetryWhile> retryWhile();

}
