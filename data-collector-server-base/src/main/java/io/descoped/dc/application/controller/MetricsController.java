package io.descoped.dc.application.controller;

import io.descoped.dc.api.http.Request;
import io.descoped.dc.application.spi.Controller;
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter;
import io.prometheus.metrics.model.registry.MetricNameFilter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

// https://github.com/prometheus/client_java#counter
/**
 * Serves {@code /metrics} in the Prometheus text format, and {@code /metrics/-/healthy}.
 */
public class MetricsController implements Controller {

    private static final String HEALTHY_RESPONSE = "Exporter is Healthy.";
    private final PrometheusRegistry registry;
    private final PrometheusTextFormatWriter writer = PrometheusTextFormatWriter.create();

    /**
     * Creates the controller.
     */
    public MetricsController() {
        registry = PrometheusRegistry.defaultRegistry;
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (exchange.getRequestPath().endsWith("/-/healthy")) {
                out.write(HEALTHY_RESPONSE.getBytes(StandardCharsets.UTF_8));
            } else {
                // no name[] gives every metric, as the simpleclient registry did
                Set<String> names = parseQuery(exchange.getQueryString());
                writer.write(out, names.isEmpty() ? registry.scrape() : registry.scrape(MetricNameFilter.builder().nameMustBeEqualTo(names).build()));
            }

            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, writer.getContentType());
            exchange.getResponseSender().send(ByteBuffer.wrap(out.toByteArray()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
