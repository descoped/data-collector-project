package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Client;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperClass;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Instruments every {@link Client} implementation and the {@code GetHandler} with the metrics advice of
 * {@link HttpClientExporter}.
 */
public class HttpClientAgent {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientAgent.class);

    private HttpClientAgent() {
    }

    /*
     * Findings with ByteBuddy 1.10.8
     * - The hasSuperClass breaks if you invoke GetHandler.class.getName(). A fully qualified name must be assigned as string!
     * - The hasSuperType accepts interface Object.class.getName()!
     *
     * The interceptors are Advice, inlined into the methods: MethodDelegation with @SuperCall needs helper classes
     * defined in the target's package, which ByteBuddy cannot do in a named module on JDK 27 (issue #25), so no
     * metric was recorded. A client in another module is given a read edge to this one.
     */
    // installed once per JVM: a second agent would inline the advice again and count everything twice
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    /**
     * Instruments the HTTP clients and the get handlers, loaded and to be loaded, through the instrumentation. Only the
     * first successful call in a JVM installs the agent; when installation fails, a later call tries again.
     *
     * @param inst the instrumentation to install the agent with
     */
    public static void install(Instrumentation inst) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            new AgentBuilder.Default()
                    .disableClassFormatChanges()
                    .with(errorLogger())
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .assureReadEdgeTo(inst, HttpClientExporter.class)
                    .type(hasSuperType(named(Client.class.getName())))
                    .transform((builder, typeDescription, classLoader, javaModule, loaded) -> builder
                            .visit(Advice.to(HttpClientExporter.Send.class).on(named("send")))
                            .visit(Advice.to(HttpClientExporter.SendAsync.class).on(named("sendAsync"))))
                    .type(hasSuperClass(named("io.descoped.dc.core.handler.GetHandler")))
                    .transform((builder, typeDescription, classLoader, javaModule, loaded) -> builder
                            .visit(Advice.to(HttpClientExporter.GetHandlerInterceptor.class).on(named("execute"))))
                    .installOn(inst);
        } catch (RuntimeException | Error e) {
            // not installed: a later call may try again
            INSTALLED.set(false);
            throw e;
        }
    }

    /**
     * A listener that logs a type the agent could not instrument, by name and the class of the error: ByteBuddy drops
     * a failed transformation silently, which is how no metric was recorded on JDK 27 without anyone noticing.
     *
     * @return the listener
     */
    public static AgentBuilder.Listener errorLogger() {
        return new AgentBuilder.Listener.Adapter() {
            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                LOG.warn("Metrics agent could not instrument {}: {}", typeName, throwable.getClass().getName());
            }
        };
    }
}
