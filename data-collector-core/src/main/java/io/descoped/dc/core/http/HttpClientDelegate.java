package io.descoped.dc.core.http;

import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Client;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The {@link Client} on the {@code java.net.http} client. Sending wraps an {@code IOException}, or an interruption
 * (with the interrupt status kept), in a {@code RuntimeException}.
 */
public class HttpClientDelegate implements Client {

    final HttpClient httpClient;

    private HttpClientDelegate(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public Version version() {
        return Version.valueOf(httpClient.version().name());
    }

    @Override
    public Response send(Request request) {
        try {
            HttpResponse<byte[]> httpResponse = httpClient.send((HttpRequest) request.getDelegate(), HttpResponse.BodyHandlers.ofByteArray());
            Response.Builder responseBuilder = Response.newResponseBuilder();
            responseBuilder.delegate(httpResponse);
            return responseBuilder.build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> Response send(Request request, BodyHandler<R> bodyHandler) {
        try {
            HttpResponse.BodyHandler<Void> handler = HttpResponse.BodyHandlers.fromSubscriber(bodyHandler);
            HttpResponse<Void> httpResponse = httpClient.send((HttpRequest) request.getDelegate(), handler);
            Response.Builder responseBuilder = Response.newResponseBuilder();
            responseBuilder.bodyHandler(bodyHandler);
            responseBuilder.delegate(httpResponse);
            return responseBuilder.build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Response> sendAsync(Request request) {
        return httpClient.sendAsync((HttpRequest) request.getDelegate(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(httpResponse -> {
                    Response.Builder responseBuilder = Response.newResponseBuilder();
                    responseBuilder.delegate(httpResponse);
                    return responseBuilder.build();
                });
    }

    @Override
    public <R> CompletableFuture<Response> sendAsync(Request request, BodyHandler<R> bodyHandler) {
        HttpResponse.BodyHandler<Void> handler = HttpResponse.BodyHandlers.fromSubscriber(bodyHandler);
        return httpClient.sendAsync((HttpRequest) request.getDelegate(), handler)
                .thenApply(httpResponse -> {
                    Response.Builder responseBuilder = Response.newResponseBuilder();
                    responseBuilder.bodyHandler(bodyHandler);
                    responseBuilder.delegate(httpResponse);
                    return responseBuilder.build();
                });
    }

    @Override
    public Object getDelegate() {
        return httpClient;
    }

    /**
     * Builds an {@link HttpClientDelegate}; the provider of {@link Client.Builder}. The default version is HTTP/2.
     * The trust manager is not kept: the client takes its trust managers from the SSL context.
     */
    public static class ClientBuilder implements Builder {

        private Version version = Version.HTTP_2;
        private int priority = 0;
        private Authenticator authenticator;
        private SSLContext sslContext;
        private SSLParameters sslParameters;
        private Executor executor;
        private Duration duration;
        private Redirect redirectPolicy;
        private ProxySelector proxySelector;

        /**
         * Creates an empty builder.
         */
        public ClientBuilder() {
            // the state is in the fields
        }

        @Override
        public Builder version(Version version) {
            this.version = version;
            return this;
        }

        @Override
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public Builder authenticator(Authenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        @Override
        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        @Override
        public Builder sslParameters(SSLParameters sslParameters) {
            this.sslParameters = sslParameters;
            return this;
        }

        @Override
        public Builder x509TrustManager(X509TrustManager trustManager) {
            // not kept: the Java HttpClient takes its trust managers from the SSLContext
            return this;
        }

        @Override
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public Builder connectTimeout(Duration duration) {
            this.duration = duration;
            return this;
        }

        @Override
        public Builder followRedirects(Redirect policy) {
            redirectPolicy = policy;
            return this;
        }

        @Override
        public Builder proxy(ProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        @Override
        public Client build() {
            HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();

            switch (version) {
                case HTTP_1_1:
                    httpClientBuilder.version(HttpClient.Version.HTTP_1_1);
                    break;

                case HTTP_2:
                    httpClientBuilder.version(HttpClient.Version.HTTP_2);
                    break;
                default:
                    throw new IllegalStateException();
            }

            if (priority > 0) httpClientBuilder.priority(priority);

            if (authenticator != null) httpClientBuilder.authenticator(authenticator);

            if (sslContext != null) {
                httpClientBuilder.sslContext(sslContext);
            }

            if (sslParameters != null) httpClientBuilder.sslParameters(sslParameters);

            if (executor != null) httpClientBuilder.executor(executor);

            if (duration != null) httpClientBuilder.connectTimeout(duration);

            if (redirectPolicy != null) {
                switch (redirectPolicy) {
                    case NEVER:
                        httpClientBuilder.followRedirects(HttpClient.Redirect.NEVER);
                        break;

                    case ALWAYS:
                        httpClientBuilder.followRedirects(HttpClient.Redirect.ALWAYS);
                        break;

                    case NORMAL:
                        httpClientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            if (proxySelector != null) httpClientBuilder.proxy(proxySelector);

            return new HttpClientDelegate(httpClientBuilder.build());
        }
    }

}
