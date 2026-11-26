package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.Request;
import io.descoped.dc.application.spi.Controller;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

// https://github.com/prometheus/client_java#counter
// https://github.com/prometheus/client_java/blob/master/simpleclient_httpserver/src/main/java/io/prometheus/client/exporter/HTTPServer.java
/**
 * Serves {@code /metrics} in the Prometheus text format, and {@code /metrics/-/healthy}.
 */
public class MetricsController implements Controller {

    private static final String HEALTHY_RESPONSE = "Exporter is Healthy.";
    private final CollectorRegistry collectorRegistry;

    /**
     * Creates the controller.
     */
    public MetricsController() {
        collectorRegistry = CollectorRegistry.defaultRegistry;
    }

    @Override
    public String contextPath() {
        return "/metrics";
    }

    @Override
    public Set<Request.Method> allowedMethods() {
        return Set.of(Request.Method.GET);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        if ("get".equalsIgnoreCase(exchange.getRequestMethod().toString()) && exchange.getRequestPath().startsWith("/metrics")) {
            dealWithMetrics(exchange);
            return;
        }

        exchange.setStatusCode(400);
    }

    private void dealWithMetrics(HttpServerExchange exchange) {
        try (StringWriter sw = new StringWriter()) {
            if (exchange.getRequestPath().endsWith("/-/healthy")) {
                sw.write(HEALTHY_RESPONSE);
            } else {
                String query = exchange.getQueryString();
                TextFormat.write004(sw, collectorRegistry.filteredMetricFamilySamples(parseQuery(query)));
            }

            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
            exchange.getResponseSender().send(sw.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The metric names asked for by {@code name[]} parameters.
     *
     * @param query the query string, or {@code null}
     * @return the names
     * @throws UnsupportedEncodingException never; UTF-8 is always supported
     */
    protected static Set<String> parseQuery(String query) throws UnsupportedEncodingException {
        Set<String> names = new HashSet<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx != -1 && URLDecoder.decode(pair.substring(0, idx), "UTF-8").equals("name[]")) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return names;
    }
}
