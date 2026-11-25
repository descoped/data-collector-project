package io.descoped.dc.test.controller;

import io.descoped.dc.api.node.FormEncoding;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthorizationResource extends AbstractResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationResource.class);

    @Override
    void handle(HttpServerExchange exchange) {
        FormDataParser parser = new FormDataParser(exchange);

        if (parser.isFormUrlEncoded()) {
            Map<String, String> nameValuePairs = parser.parseUrlEncodedNameValuePairs();
            LOG.info("Authorize: {} [mock notice: credentials are ignored]", nameValuePairs);
            String json = "{\"access_token\": \"SECRET\"}";
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.getResponseSender().send(json);
        }
    }

    static class FormDataParser {
        private final HttpServerExchange exchange;
        private final String contentType;

        public FormDataParser(HttpServerExchange exchange) {
            this.exchange = exchange;
            this.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        }

        boolean isPlainText() {
            return !(isFormUrlEncoded() || isMultiFormData());
        }

        boolean isFormUrlEncoded() {
            return contentType.startsWith(FormEncoding.APPLICATION_X_WWW_FORM_URLENCODED.getMimeType());
        }

        boolean isMultiFormData() {
            return contentType.startsWith(FormEncoding.MULTIPART_FORM_DATA.getMimeType());
        }

        // null for a request that is neither form url encoded nor multipart; a multipart parser keeps its files in the
        // default temporary directory (java.io.tmpdir)
        private io.undertow.server.handlers.form.FormDataParser formParser(final HttpServerExchange exchange, final String charset) {
            if (isFormUrlEncoded()) {
                return new FormEncodedDataDefinition()
                        .setDefaultEncoding(charset)
                        .create(exchange);
            } else if (isMultiFormData()) {
                return new MultiPartParserDefinition()
                        .setDefaultEncoding(charset)
                        .create(exchange);
            }
            return null;
        }

        // the values are read before the parser is closed
        Map<String, String> parseUrlEncodedNameValuePairs() {
            Map<String, String> map = new LinkedHashMap<>();
            try (io.undertow.server.handlers.form.FormDataParser parser = formParser(exchange, "UTF-8")) {
                FormData formData = parser != null ? parser.parseBlocking() : new FormData(0);
                for (String name : formData) {
                    String value = formData.get(name).stream().findFirst().map(FormData.FormValue::getValue).orElse(null);
                    map.put(name, value);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return map;
        }

    }
}
