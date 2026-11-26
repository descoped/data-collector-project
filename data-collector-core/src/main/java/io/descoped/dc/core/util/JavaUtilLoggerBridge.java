package io.descoped.dc.core.util;

import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.logging.LogManager;

/**
 * Routes {@code java.util.logging} to SLF4J.
 */
public class JavaUtilLoggerBridge {

    private JavaUtilLoggerBridge() {
    }

    private static class Initializer {
        static {
            LogManager.getLogManager().reset();
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
        }

        private static void configure() {
            // method exists to allow access to static initializer
        }
    }

    /**
     * Resets {@code java.util.logging}, removes the root logger's handlers and installs the SLF4J bridge handler.
     * Only the first call has an effect.
     */
    public static final void installJavaUtilLoggerBridgeHandler() {
        Initializer.configure();
    }
}
