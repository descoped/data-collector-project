package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.CompositionHandler;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.SupportHandler;
import io.descoped.dc.api.node.Base;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry of the handlers: it finds the handler classes of this package by their {@code Handler},
 * {@code SupportHandler} and {@code CompositionHandler} annotations, and creates a handler for a node.
 */
public class Handlers {

    private final Map<Class<? extends Base>, Class<?>> handlerFactory = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> supportHandlerFactory = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> compositionHandlerFactory = new ConcurrentHashMap<>();

    /**
     * Creates the class; the handlers are reached through its static methods.
     */
    public Handlers() {
        // the state is in the fields
    }

    private static Handlers instance() {
        return NodeFactoryHolder.INSTANCE;
    }

    // the handlers are registered by their annotations: the class of a node's handler is an AbstractHandler of that node
    /**
     * Creates the handler of a node, registered for the node's type.
     *
     * @param node the node
     * @param <N>  the type of the node
     * @return a new handler of the node
     * @throws IllegalStateException if no handler is registered for the node's type
     * @throws RuntimeException      if the handler cannot be created
     */
    @SuppressWarnings("unchecked")
    public static <N extends Base> AbstractHandler<N> createHandlerFor(N node) {
        try {
            Class<? extends Base> nodeClass = (Class<? extends Base>) node.getClass().getInterfaces()[0];
            Class<?> handlerClass = instance().handlerFactory.get(nodeClass);
            if (handlerClass == null) {
                throw new IllegalStateException("unable to resolve handler for: " + nodeClass);
            }

            Constructor<?> constructor = handlerClass.getDeclaredConstructor(nodeClass);
            return (AbstractHandler<N>) constructor.newInstance(node);

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the support handler registered for a node type and a selector class, with its no-argument constructor.
     *
     * @param forNodeClass  the node type
     * @param selectorClass the selector class
     * @param <R>           the type of the support handler
     * @return a new support handler
     * @throws IllegalStateException if no support handler is registered for the pair
     * @throws RuntimeException      if the support handler cannot be created
     */
    @SuppressWarnings("unchecked") // the caller names the support handler's type
    public static <R> R createSupportHandlerFor(Class<? extends Base> forNodeClass, Class<?> selectorClass) {
        try {
            String classes = createHandlerClasses(forNodeClass, selectorClass);
            Class<?> supportHandlerClass = instance().supportHandlerFactory.get(classes);
            if (supportHandlerClass == null) {
                throw new IllegalStateException("unable to resolve handler for: " + forNodeClass);
            }
            /*
             * Create new node handler
             */
            Constructor<?> constructor = supportHandlerClass.getDeclaredConstructor();
            return (R) constructor.newInstance();

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates the composition handler registered for a node type and a selector node type, for a node.
     *
     * @param node              the node
     * @param forNodeClass      the node type
     * @param selectorNodeClass the selector node type
     * @param <N>               the type of the node
     * @return a new composition handler of the node
     * @throws IllegalStateException if no composition handler is registered for the pair
     * @throws RuntimeException      if the composition handler cannot be created
     */
    @SuppressWarnings("unchecked") // as createHandlerFor
    public static <N extends Base> AbstractHandler<N> createCompositionHandlerFor(N node,
                                                                                  Class<? extends Base> forNodeClass,
                                                                                  Class<? extends Base> selectorNodeClass) {
        try {
            String classes = createHandlerClasses(forNodeClass, selectorNodeClass);
            Class<?> compositionHandlerClass = instance().compositionHandlerFactory.get(classes);
            if (compositionHandlerClass == null) {
                throw new IllegalStateException("unable to resolve handler for: " + forNodeClass);
            }
            /*
             * Create new node handler
             */
            Constructor<?> constructor = compositionHandlerClass.getDeclaredConstructor(forNodeClass);
            return (AbstractHandler<N>) constructor.newInstance(node);

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static String createHandlerClasses(Class<?> nodeClass, Class<?> byClass) {
        return nodeClass.getName() + " " + byClass.getName();
    }

    private void registerHandler(Class<? extends Base> nodeClass, Class<?> nodeHandlerClass) {
        handlerFactory.put(nodeClass, nodeHandlerClass);
    }

    private void registerSupportHandler(String nodeAndSelectorClass, Class<?> nodeSupportHandlerClass) {
        supportHandlerFactory.put(nodeAndSelectorClass, nodeSupportHandlerClass);
    }

    private void registerCompositionHandler(String nodeClasses, Class<?> nodeHandlerClass) {
        compositionHandlerFactory.put(nodeClasses, nodeHandlerClass);
    }

    private static final class NodeFactoryHolder {
        private static final Handlers INSTANCE = new Handlers();

        /*
         * Discover node handlers
         */
        static {
            nodeHandlerDiscovery(Handlers.class.getPackageName()).forEach(nodeHandlerClass -> {
                if (!(nodeHandlerClass.isAnnotationPresent(Handler.class) || nodeHandlerClass.isAnnotationPresent(SupportHandler.class) || nodeHandlerClass.isAnnotationPresent(CompositionHandler.class))) {
                    throw new IllegalStateException("Handler is not declared for clazz: " + nodeHandlerClass);
                }

                if (nodeHandlerClass.isAnnotationPresent(Handler.class)) {
                    Class<? extends Base> nodeClass = nodeHandlerClass.getAnnotation(Handler.class).forClass();
                    instance().registerHandler(nodeClass, nodeHandlerClass);
                }

                if (nodeHandlerClass.isAnnotationPresent(SupportHandler.class)) {
                    SupportHandler supportHandlerAnnotation = nodeHandlerClass.getAnnotation(SupportHandler.class);
                    Class<? extends Base> nodeClass = supportHandlerAnnotation.forClass();
                    Class<?> byClass = supportHandlerAnnotation.selectorClass();
                    String classes = createHandlerClasses(nodeClass, byClass);
                    instance().registerSupportHandler(classes, nodeHandlerClass);
                }

                if (nodeHandlerClass.isAnnotationPresent(CompositionHandler.class)) {
                    CompositionHandler compositionHandlerAnnotation = nodeHandlerClass.getAnnotation(CompositionHandler.class);
                    Class<? extends Base> nodeClass = compositionHandlerAnnotation.forClass();
                    Class<? extends Base> byClass = compositionHandlerAnnotation.selectorClass();
                    String classes = createHandlerClasses(nodeClass, byClass);
                    instance().registerCompositionHandler(classes, nodeHandlerClass);
                }
            });
        }

        static Set<Class<?>> nodeHandlerDiscovery(String... packageNames) {
            // ClassGraph 6 scans only the sources it is given: the class path and the module path, as 4.x did
            try (ScanResult scanResult = new ClassGraph().enableClasspath().enableNonSystemModules().enableClassInfo().enableAnnotationInfo()
                    .acceptPackages(packageNames).scan()) {

                ClassInfoList classInfoHandlerList = scanResult.getClassesWithAnnotation(Handler.class.getName());
                ClassInfoList classInfoSupportHandlerList = scanResult.getClassesWithAnnotation(SupportHandler.class.getName());
                ClassInfoList classInfoCompositionHandlerList = scanResult.getClassesWithAnnotation(CompositionHandler.class.getName());

                Set<Class<?>> classes = new HashSet<>();

                for (ClassInfo classInfo : classInfoHandlerList) {
                    classes.add(loadClass(classInfo.getName()));
                }

                for (ClassInfo classInfo : classInfoSupportHandlerList) {
                    classes.add(loadClass(classInfo.getName()));
                }

                for (ClassInfo classInfo : classInfoCompositionHandlerList) {
                    classes.add(loadClass(classInfo.getName()));
                }
                return classes;
            }
        }

        // ClassGraph 6 does not load classes; these are dc's own handlers, found by the scan
        private static Class<?> loadClass(String name) {
            try {
                return Class.forName(name, false, Handlers.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Could not load " + name, e);
            }
        }
    }
}
