package io.descoped.dc.api.node;

import io.descoped.dc.api.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * A validator of the response's status code.
 */
public interface HttpStatusValidation extends Validator {

    /**
     * The status codes that are a success, each with the response predicates that must hold as well.
     *
     * @return the status codes and their predicates
     */
    Map<HttpStatus, List<ResponsePredicate>> success();

    /**
     * The status codes that are a failure.
     *
     * @return the status codes
     */
    List<HttpStatus> failed();

}
