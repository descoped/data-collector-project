package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Client;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperClass;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class HttpClientAgent {

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

    public static void install(Instrumentation inst) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        new AgentBuilder.Default()
                .disableClassFormatChanges()
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
    }
}
