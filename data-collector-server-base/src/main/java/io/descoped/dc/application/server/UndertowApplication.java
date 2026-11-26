package io.descoped.dc.application.server;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.services.InjectionParameters;
import io.descoped.dc.api.services.ObjectCreator;
import io.descoped.dc.application.controller.CORSHandler;
import io.descoped.dc.application.controller.DispatchController;
import io.descoped.dc.application.health.HealthApplicationMonitor;
import io.descoped.dc.application.health.HealthApplicationResource;
import io.descoped.dc.application.health.HealthConfigResource;
import io.descoped.dc.application.health.HealthContextsResource;
import io.descoped.dc.application.health.HealthResourceFactory;
import io.descoped.dc.application.metrics.MetricsResourceFactory;
import io.descoped.dc.application.security.AuthenticationHandler;
import io.descoped.dc.application.spi.Authenticator;
import io.descoped.dc.application.spi.Component;
import io.descoped.dc.application.spi.Controller;
import io.descoped.dc.application.spi.Service;
import io.descoped.dc.application.ssl.BusinessSSLResourceSupplier;
import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The data collector application on Undertow: its controllers, components and services.
 */
public class UndertowApplication {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowApplication.class);

    private final Undertow server;
    private final String host;
    private final int port;
    private final Map<Class<? extends Component>, Component> components;
    private final Map<Class<? extends Service>, Service> services;
    private final HealthApplicationMonitor applicationMonitor;

    private UndertowApplication(DynamicConfiguration configuration,
                                String host,
                                int port,
                                DispatchController dispatchController,
                                Map<Class<? extends Component>, Component> components,
                                Map<Class<? extends Service>, Service> services,
                                HealthApplicationMonitor applicationMonitor) {
        this.host = host;
        this.port = port;
        this.components = components;
        this.services = services;
        this.applicationMonitor = applicationMonitor;

        PathHandler pathHandler = Handlers.path();

        pathHandler.addPrefixPath("/", dispatchController);

        // every request is authenticated; CORS, when it is configured, answers preflights before that
        HttpHandler handler = new AuthenticationHandler(authenticator(configuration), pathHandler);
        if (!configuration.get("http.cors.allow.origin", "").isBlank()) {
            handler = configureCORSHandler(configuration, handler);
        }

        this.server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(handler)
                .build();
        this.applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZED);
    }

    // the provider named by http.authentication.provider (default bearer), initialized; the server does not start
    // without one
    static Authenticator authenticator(DynamicConfiguration configuration) {
        String name = configuration.get("http.authentication.provider", "bearer");
        Authenticator authenticator = select(ServiceLoader.load(Authenticator.class, UndertowApplication.class.getClassLoader())
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList(), name);
        authenticator.initialize(configuration);
        LOG.info("Authentication provider: {}", name);
        return authenticator;
    }

    // exactly one provider has the name: a second jar must not replace the authenticator unnoticed
    static Authenticator select(List<Authenticator> providers, String name) {
        List<Authenticator> named = providers.stream().filter(provider -> name.equals(provider.name())).toList();
        if (named.isEmpty()) {
            throw new IllegalStateException("No authentication provider named: " + name);
        }
        if (named.size() > 1) {
            throw new IllegalStateException("More than one authentication provider named: " + name);
        }
        return named.get(0);
    }

    private CORSHandler configureCORSHandler(DynamicConfiguration configuration, HttpHandler next) {
        List<Pattern> corsAllowOrigin = Stream.of(configuration.get("http.cors.allow.origin", "")
                .split(",")).map(Pattern::compile).toList();
        Set<String> corsAllowMethods = Set.of(configuration.get("http.cors.allow.methods", "POST,GET,PUT,DELETE,HEAD")
                .split(","));
        Set<String> corsAllowHeaders = Set.of(configuration.get("http.cors.allow.header", "Content-Type,Authorization")
                .split(","));
        boolean corsAllowCredentials = Boolean.parseBoolean(configuration.find("http.cors.allow.credentials").orElse(null));
        int corsMaxAge = configuration.find("http.cors.allow.max-age").map(Integer::parseInt).orElse(900);

        return new CORSHandler(next, next, corsAllowOrigin,
                corsAllowCredentials, StatusCodes.NO_CONTENT, corsMaxAge, corsAllowMethods, corsAllowHeaders
        );
    }

    /**
     * Initializes the application on the port in {@code http.port}.
     *
     * @param configuration               the configuration
     * @param businessSSLResourceSupplier the business certificate resource, or {@code null}
     * @return the application, not started
     */
    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration,
                                                                    BusinessSSLResourceSupplier businessSSLResourceSupplier) {
        int port = Integer.parseInt(configuration.find("http.port").orElse(null));
        return initializeUndertowApplication(configuration, port, businessSSLResourceSupplier);
    }

    /**
     * Initializes the application on a port.
     *
     * @param configuration               the configuration
     * @param port                        the port
     * @param businessSSLResourceSupplier the business certificate resource, or {@code null}
     * @return the application, not started
     */
    public static UndertowApplication initializeUndertowApplication(DynamicConfiguration configuration,
                                                                    Integer port,
                                                                    BusinessSSLResourceSupplier businessSSLResourceSupplier) {
        if (!Boolean.parseBoolean(configuration.find("prometheus.defaultExports.disabled").orElse(null))) {
            DefaultExports.initialize();
        }
        LOG.info("Initializing Data Collector server ...");
        if (businessSSLResourceSupplier != null && LOG.isInfoEnabled()) {
            LOG.info("Use delegated certificate provider: {}", configuration.find("data.collector.sslBundle.provider").orElse(null));
        }

        MetricsResourceFactory metricsResourceFactory = MetricsResourceFactory.create();

        HealthResourceFactory healthResourceFactory = HealthResourceFactory.create();
        healthResourceFactory.getHealthResource(HealthConfigResource.class).setConfiguration(configuration.asMap());
        HealthApplicationMonitor applicationMonitor = healthResourceFactory.getHealthResource(HealthApplicationResource.class).getMonitor();
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.INITIALIZING);

        String host = configuration.get("http.host", "localhost");

        applicationMonitor.setHost(host);
        applicationMonitor.setPort(port);

        // scan and register Components
        InjectionParameters componentInjectionParameters = new InjectionParameters();
        componentInjectionParameters.register(DynamicConfiguration.class, configuration);
        componentInjectionParameters.register(BusinessSSLResourceSupplier.class, businessSSLResourceSupplier);
        Map<Class<? extends Component>, Component> components = new LinkedHashMap<>();
        for (Class<Component> componentClass : ServiceProviderDiscovery.discover(Component.class)) {
            Component component = ObjectCreator.newInstance(componentClass, componentInjectionParameters);
            component.initialize();
            componentInjectionParameters.register(componentClass, component);
            components.put(componentClass, component);
            LOG.info("Registered component: {}", componentClass.getName());
        }

        // scan and register Services and copy all components to service injection params
        InjectionParameters serviceInjectionParameters = new InjectionParameters();
        serviceInjectionParameters.putAll(componentInjectionParameters);
        serviceInjectionParameters.register(MetricsResourceFactory.class, metricsResourceFactory);
        serviceInjectionParameters.register(HealthResourceFactory.class, healthResourceFactory);

        InjectionParameters controllerInjectionParameters = new InjectionParameters();
        controllerInjectionParameters.putAll(serviceInjectionParameters);

        Map<Class<? extends Service>, Service> services = new LinkedHashMap<>();
        for (Class<Service> serviceClass : ServiceProviderDiscovery.discover(Service.class)) {
            Service service = ObjectCreator.newInstance(serviceClass, serviceInjectionParameters);
            if (service.isEnabled()) {
                controllerInjectionParameters.register(serviceClass, service);
                services.put(serviceClass, service);
                LOG.info("Registered service: {}", serviceClass.getName());
            }
        }

        // scan and register Controllers and copy all services to controller injection params
        HealthContextsResource healthContextsResource = healthResourceFactory.getHealthResource(HealthContextsResource.class);
        NavigableMap<String, Controller> controllers = new TreeMap<>();
        for (Class<Controller> controllerClass : ServiceProviderDiscovery.discover(Controller.class)) {
            Controller controller = ObjectCreator.newInstance(controllerClass, controllerInjectionParameters);
            String contextPath = controller.contextPath();
            controllers.put(contextPath, controller);
            healthContextsResource.add(contextPath, controller.allowedMethods(), controllerClass);
            LOG.info("Registered controller: {} ->  {}", contextPath, controller.getClass().getName());
        }

        DispatchController dispatchController = new DispatchController(controllers);

        return new UndertowApplication(configuration, host, port, dispatchController, components, services, applicationMonitor);
    }

    /**
     * The host the server listens on.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * The port the server listens on.
     *
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Starts the server.
     */
    public void start() {
        server.start();
        enableAllServices();
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.RUNNING);
        LOG.info("Started Data Collector. PID {}", ProcessHandle.current().pid());
        LOG.info("Listening on {}:{}", host, port);
    }

    /**
     * Stops the server, its services and components.
     */
    public void stop() {
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.SHUTTING_DOWN);
        server.stop();
        for (Service service : services.values()) {
            service.stop();
        }
        for (Component component : components.values()) {
            try {
                component.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        applicationMonitor.setServerStatus(HealthApplicationMonitor.ServerStatus.SHUTDOWN);
        LOG.info("Leaving.. Bye!");
    }

    /**
     * Starts every service.
     */
    public void enableAllServices() {
        for (Service service : services.values()) {
            LOG.info("Starting: {}", service.getClass().getName());
            service.start();
        }
    }

    /**
     * Starts a service.
     *
     * @param service the service's class
     */
    public void enable(Class<? extends Service> service) {
        services.get(service).start();
    }

    /**
     * Stops a service.
     *
     * @param service the service's class
     */
    public void disable(Class<? extends Service> service) {
        services.get(service).stop();
    }

    /**
     * The server, a component or a service of a class.
     *
     * @param clazz the class
     * @param <R>   the type
     * @return the object
     * @throws UnsupportedOperationException if the application has none of the class
     */
    public <R> R unwrap(Class<R> clazz) {
        if (clazz.isAssignableFrom(server.getClass())) {
            return clazz.cast(server);
        } else {
            if (components.containsKey(clazz)) {
                return clazz.cast(components.get(clazz));
            }
            if (services.containsKey(clazz)) {
                return clazz.cast(services.get(clazz));
            }
            throw new UnsupportedOperationException("Unable to unwrap class: " + clazz);
        }
    }

}
