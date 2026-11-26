/**
 * The data collector application on Undertow: service discovery, routing, CORS, health and Prometheus metrics.
 */
module io.descoped.dc.application {

    requires io.descoped.service.provider.api;
    requires transitive io.descoped.dynamic.config;
    requires transitive io.descoped.dc.api;
    requires io.descoped.secrets.api;

    requires org.slf4j;
    requires io.github.classgraph;
    requires com.fasterxml.jackson.databind;

    requires transitive undertow.core;
    requires io.prometheus.metrics.core;
    requires io.prometheus.metrics.model;
    requires io.prometheus.writer.text;
    requires io.prometheus.metrics.instrumentation.jvm;

    requires freemarker;

    uses io.descoped.dc.application.spi.Authenticator;
    provides io.descoped.dc.application.spi.Authenticator
            with io.descoped.dc.application.security.BearerTokenAuthenticator;

    opens io.descoped.dc.application.health to com.fasterxml.jackson.databind;
    opens io.descoped.dc.application.engine; // open to unnamed module freemarker
    opens io.descoped.dc.application.metrics;

    exports io.descoped.dc.application.spi;
    exports io.descoped.dc.application.security;
    exports io.descoped.dc.application.ssl;
    exports io.descoped.dc.application.server;
    exports io.descoped.dc.application.controller;
    exports io.descoped.dc.application.engine;
    exports io.descoped.dc.application.health;
    exports io.descoped.dc.application.metrics;
}
