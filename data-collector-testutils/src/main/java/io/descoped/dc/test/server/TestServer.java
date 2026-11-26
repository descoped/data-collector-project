package io.descoped.dc.test.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.security.BearerTokenAuthenticator;
import io.descoped.dc.application.server.UndertowApplication;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;
import io.descoped.dc.application.ssl.SecretManagerSSLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A data collector server for tests, on a port of its own on localhost.
 */
public class TestServer implements TestUriResolver {

    static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private final DynamicConfiguration configuration;

    private final UndertowApplication application;

    private final String authorization;

    /**
     * Creates the server; it is not started.
     *
     * @param configuration         the server's configuration
     * @param testServerServicePort the port, or {@code 0} for a free port that the system chooses when the server starts
     */
    public TestServer(DynamicConfiguration configuration, int testServerServicePort) {
        // the test authenticator with a token of this server's own, unless the configuration names others
        String token = UUID.randomUUID().toString();
        DynamicConfiguration effective = DynamicConfiguration.builder()
                .value("http.authentication.provider", "test")
                .value(BearerTokenAuthenticator.TOKEN_KEY, token)
                .values(configuration.asMap())
                .build();
        this.configuration = configuration;
        this.authorization = "Bearer " + effective.find(BearerTokenAuthenticator.TOKEN_KEY).orElse(token);

        /*
         * Delegated certificate loader from Google Secret Manager is placed here, because we don't won't a
         * dependency in the data collector to any host aware libraries such as the Google Secret Manager.
         * The supplier is wrapped by the BusinessSSLResourceSupplier and passes an implementing instance of
         * BusinessSSLBundle to Worker.useBusinessSSLResourceSupplier() in the Core module.
         *
         * Please note: only Google Secret Manager is supported!
         */

        String businessSslResourceProvider = effective.find("data.collector.sslBundle.provider").orElse(null);
        Supplier<SecretManagerSSLResource> sslResourceSupplier = () -> new SecretManagerSSLResource(effective);

        application = UndertowApplication.initializeUndertowApplication(effective, testServerServicePort,
                businessSslResourceProvider != null ? new BusinessSSLResourceSupplier(sslResourceSupplier) : null);
    }

    /**
     * Creates a server on a free port, which the system chooses when the server starts.
     *
     * @param configuration the server's configuration
     * @return the server, not started
     */
    public static TestServer create(DynamicConfiguration configuration) {
        return new TestServer(configuration, 0);
    }

    /**
     * Starts the server.
     */
    public void start() {
        application.start();
    }

    /**
     * Stops the server.
     */
    public void stop() {
        application.stop();
    }

    /**
     * The server's host.
     *
     * @return the host
     */
    public String getTestServerHost() {
        return application.getHost();
    }

    /**
     * The server's port.
     *
     * @return the port
     */
    public int getTestServerServicePort() {
        return application.getPort();
    }

    @Override
    public Optional<String> authorization() {
        return Optional.of(authorization);
    }

    @Override
    public String testURL(String uri) {
        // as URL(protocol, host, port, file).toExternalForm() wrote it: the path is not parsed, since it may hold
        // ${..} expressions
        String host = application.getHost();
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        return "http://" + host + ":" + application.getPort() + uri;
    }

    /**
     * The server's configuration.
     *
     * @return the configuration
     */
    public DynamicConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * The application the server runs.
     *
     * @return the application
     */
    public UndertowApplication getApplication() {
        return application;
    }

}
