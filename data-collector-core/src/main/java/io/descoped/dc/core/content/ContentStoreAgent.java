package io.descoped.dc.core.content;

import io.descoped.dc.api.content.ContentStore;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class ContentStoreAgent {

    private ContentStoreAgent() {
    }

    // installed once per JVM: a second agent would inline the advice again and count everything twice
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    public static void install(Instrumentation inst) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        // Advice, inlined into the content stores (as HttpClientAgent); a store in a connector module is given a read
        // edge to this one
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .assureReadEdgeTo(inst, ContentStoreExporter.class)
                .type(hasSuperType(named(ContentStore.class.getName())))
                .transform((builder, typeDescription, classLoader, javaModule, loaded) -> builder
                        .visit(Advice.to(ContentStoreExporter.Paginate.class).on(named("addPaginationDocument")))
                        .visit(Advice.to(ContentStoreExporter.Entry.class).on(named("bufferPaginationEntryDocument")))
                        .visit(Advice.to(ContentStoreExporter.Document.class).on(named("bufferDocument")))
                        .visit(Advice.to(ContentStoreExporter.Publish.class).on(named("publish")))
                )
                .installOn(inst);
    }
}
