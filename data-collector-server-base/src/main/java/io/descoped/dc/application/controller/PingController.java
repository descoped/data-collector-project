package io.descoped.dc.application.controller;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.net.HttpURLConnection;

/**
 * Answers {@code /ping} with 200.
 */
public class PingController implements HttpHandler {

    /**
     * Creates the controller.
     */
    public PingController() {
        // the state is in the fields
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.setStatusCode(HttpURLConnection.HTTP_OK);
    }
}
