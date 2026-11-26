package io.descoped.dc.core.handler;

import io.descoped.dc.api.error.ExecutionException;

public class HttpRequestTimeoutException extends ExecutionException {

    private static final long serialVersionUID = 1L;
    public HttpRequestTimeoutException(String message) {
        super(message);
    }
}
