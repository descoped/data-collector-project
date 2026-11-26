package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.HttpStatus;
import io.undertow.server.HttpServerExchange;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A routed request: its exchange, its path variables, and the status code and outcome the callback sets.
 */
public class PathHandler {

    private final HttpServerExchange exchange;
    private final Map<String, String> parameters;
    private final Map<String, Object> outcome = new ConcurrentHashMap<>();
    private HttpStatus statusCode;

    /**
     * Creates the handler.
     *
     * @param exchange   the exchange
     * @param parameters the path variables
     */
    public PathHandler(HttpServerExchange exchange, Map<String, String> parameters) {
        this.exchange = exchange;
        this.parameters = parameters;
    }

    /**
     * The exchange.
     *
     * @return the exchange
     */
    public HttpServerExchange exchange() {
        return exchange;
    }

    /**
     * The path variables.
     *
     * @return the values by variable name
     */
    public Map<String, String> parameters() {
        return parameters;
    }

    /**
     * What the callback returns to the client.
     *
     * @return the outcome
     */
    public Map<String, Object> outcome() {
        return outcome;
    }

    /**
     * The status code the callback set.
     *
     * @return the status code
     */
    public HttpStatus statusCode() {
        return this.statusCode;
    }

    /**
     * Sets the status code.
     *
     * @param statusCode the status code
     */
    public void statusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}
