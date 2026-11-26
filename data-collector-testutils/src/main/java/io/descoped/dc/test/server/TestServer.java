package io.descoped.dc.test.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.server.UndertowApplication;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;
import io.descoped.dc.application.ssl.SecretManagerSSLResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.function.Supplier;

/**
 * A data collector server for tests, on a port of its own on localhost.
 */
public class TestServer implements TestUriResolver {

    static final Logger LOG = LoggerFactory.getLogger(TestServer.class);

    private final DynamicConfiguration configuration;

    private final UndertowApplication application;

    private final int testServerServicePort;

    /**
     * Creates the server; it is not started.
     *
     * @param configuration         the server's configuration
     * @param testServerServicePort the port
     */
    public TestServer(DynamicConfiguration configuration, int testServerServicePort) {
        this.configuration = configuration;
        this.testServerServicePort = testServerServicePort;

        /*
         * Delegated certificate loader from Google Secret Manager is placed here, because we don't won't a
         * dependency in the data collector to any host aware libraries such as the Google Secret Manager.
         * The supplier is wrapped by the BusinessSSLResourceSupplier and passes an implementing instance of
         * BusinessSSLBundle to Worker.useBusinessSSLResourceSupplier() in the Core module.
         *
         * Please note: only Google Secret Manager is supported!
         */

        String businessSslResourceProvider = configuration.find("data.collector.sslBundle.provider").orElse(null);
        Supplier<SecretManagerSSLResource> sslResourceSupplier = () -> new SecretManagerSSLResource(configuration);

        application = UndertowApplication.initializeUndertowApplication(configuration, testServerServicePort,
                businessSslResourceProvider != null ? new BusinessSSLResourceSupplier(sslResourceSupplier) : null);
    }

    /**
     * Creates a server on a free port between 9000 and 9499.
     *
     * @param configuration the server's configuration
     * @return the server, not started
     */
    public static TestServer create(DynamicConfiguration configuration) {
        return new TestServer(configuration, TestServerFactory.findFreePort(new SecureRandom(), 9000, 9499));
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
        return testServerServicePort;
    }

    @Override
    public String testURL(String uri) {
        try {
            URL url = new URL("http", application.getHost(), application.getPort(), uri);
            return url.toExternalForm();
        } catch (MalformedURLException e) {
            throw new TestServerException(e);
        }
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
