package io.descoped.dc.api.node;

import io.descoped.dc.api.http.Headers;

import java.util.List;

// common interface for http operations
/**
 * An HTTP operation: a request, the validation of its response and the steps run on the response.
 */
public interface Operation extends NodeWithId {

    /**
     * The request URL; it may hold expressions.
     *
     * @return the URL
     */
    String url();

    /**
     * The request headers.
     *
     * @return the headers
     */
    Headers headers();

    /**
     * The validators of the response.
     *
     * @return the validators
     */
    List<Validator> responseValidators();

    /**
     * The steps run on the response.
     *
     * @return the steps
     */
    List<? extends Node> steps();

    /**
     * The names of the variables the operation returns.
     *
     * @return the names
     */
    List<String> returnVariables();

}
