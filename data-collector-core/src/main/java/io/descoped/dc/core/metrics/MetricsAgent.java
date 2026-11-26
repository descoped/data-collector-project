package io.descoped.dc.core.metrics;

import io.descoped.dc.core.content.ContentStoreAgent;
import io.descoped.dc.core.http.HttpClientAgent;

import java.lang.instrument.Instrumentation;

/**
 * The Java agent that installs the metrics instrumentation of the HTTP client and the content stores.
 */
public class MetricsAgent {

    private MetricsAgent() {
    }

    /**
     * Installs {@link HttpClientAgent} and {@link ContentStoreAgent} before the application starts.
     *
     * @param agentArgs the agent's arguments, not used
     * @param inst      the instrumentation to install the agents with
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        HttpClientAgent.install(inst);
        ContentStoreAgent.install(inst);
    }

}
