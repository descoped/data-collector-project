package io.descoped.dc.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.application.server.Configurations;
import io.descoped.dc.application.server.UndertowApplication;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;
import io.descoped.dc.application.ssl.SecretManagerSSLResource;
import io.descoped.dc.core.util.JavaUtilLoggerBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * The data collector server: reads the configuration and runs the application until it is stopped.
 */
public class Server {

    /**
     * Creates the class; the server is started by {@link #main(String[])}.
     */
    public Server() {
        // the server is started by main
    }

    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    private static final ConfigurationOverride configurationOverride = ConfigurationOverride.NONE;

    /**
     * Starts the server.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        long now = System.currentTimeMillis();

        String logbackConfigurationFile = System.getenv("LOGBACK_CONFIGURATION_FILE");
        if (logbackConfigurationFile != null) {
            System.setProperty("log4j.configurationFile", logbackConfigurationFile);
        }
        JavaUtilLoggerBridge.installJavaUtilLoggerBridgeHandler();
        if (logbackConfigurationFile != null) {
            LOG.debug("Using logback configuration: {}", logbackConfigurationFile);
        }

        DynamicConfiguration.Builder configurationBuilder = DynamicConfiguration.builder()
                .source(Configurations.propertiesResource("application-defaults.properties"))
                .source(Configurations.propertiesResource("/conf/application-defaults.properties"))
                .source(Configurations.propertiesResource("/conf/application.properties"))
                .source(Configurations.propertiesResource("/conf/application-override.properties"));
        /*
         * pre-req: `docker-compose up` in this repo
         */
        if (configurationOverride == ConfigurationOverride.DEBUG_USING_POSTGRES) {
            configurationBuilder.source(Configurations.propertiesResource("application-integration.properties"));
        }

        /*
         * pre-req: refer to examples folder and configure gcs
         */
        if (configurationOverride == ConfigurationOverride.DEBUG_USING_GCS) {
            configurationBuilder.source(Configurations.values(IntegrationTestProfile.loadApplicationPropertiesFromConsumerSpecificationProfile("gcs",
                    "data.collector.certs.directory", "gcs.service-account.key-file", "local-temp-folder")));
        }

        configurationBuilder
                .source(Configurations.environment("DC_"))
                .source(Configurations.systemProperties());

        DynamicConfiguration configuration = configurationBuilder.build();

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

        UndertowApplication application = UndertowApplication.initializeUndertowApplication(configuration,
                businessSslResourceProvider != null ? new BusinessSSLResourceSupplier(sslResourceSupplier) : null);

        boolean interrupted = false;
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOG.warn("ShutdownHook triggered..");
                application.stop();
            }));

            application.start();

            long time = System.currentTimeMillis() - now;
            LOG.info("Server started in {}ms..", time);

            // wait for termination signal
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                // the termination signal: the application is stopped first (finally), then the flag is restored
                interrupted = true;
            }
        } finally {
            application.stop();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    enum ConfigurationOverride {
        NONE,
        DEBUG_USING_POSTGRES,
        DEBUG_USING_GCS;
    }
}
