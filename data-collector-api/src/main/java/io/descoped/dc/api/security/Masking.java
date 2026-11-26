package io.descoped.dc.api.security;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.http.Headers;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Keeps secrets out of logs, exception messages, health output and message manifests.
 * <p>
 * A header is secret when its name is {@code Authorization}, {@code Proxy-Authorization}, {@code Cookie} or
 * {@code Set-Cookie}, when it contains {@code key}, {@code token}, {@code secret}, {@code signature} or
 * {@code password} (any case), or when it is named in {@value #SECRET_HEADERS_KEY} (comma-separated), for the services
 * whose security schemes use headers of their own. A secret header keeps its name; its values become {@value #MASK}.
 * <p>
 * URLs keep scheme, host, path and the names of query parameters; query values and user information are masked, and
 * the fragment is left out. Bodies are described by their length and content type, never by their content.
 */
public final class Masking {

    /**
     * The configuration key that names further secret headers, comma-separated.
     */
    public static final String SECRET_HEADERS_KEY = "data.collector.secret-headers";

    /**
     * What a secret value is replaced by.
     */
    public static final String MASK = "****";

    private static final Set<String> SECRET_HEADERS = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");
    private static final List<String> SECRET_WORDS = List.of("key", "token", "secret", "signature", "password");
    private static final Masking DEFAULTS = new Masking(Set.of());

    private final Set<String> additionalHeaders;

    private Masking(Set<String> additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
    }

    /**
     * The default secret headers only.
     *
     * @return the masking
     */
    public static Masking defaults() {
        return DEFAULTS;
    }

    /**
     * The default secret headers and those named in {@value #SECRET_HEADERS_KEY}.
     *
     * @param configuration the configuration, or {@code null} for the defaults
     * @return the masking
     */
    public static Masking of(ConfigurationMap configuration) {
        return configuration == null ? DEFAULTS : of(configuration.get(SECRET_HEADERS_KEY));
    }

    /**
     * The default secret headers and the given ones.
     *
     * @param additionalHeaders further secret header names, comma-separated, or {@code null}
     * @return the masking
     */
    public static Masking of(String additionalHeaders) {
        if (additionalHeaders == null || additionalHeaders.isBlank()) {
            return DEFAULTS;
        }
        return new Masking(Arrays.stream(additionalHeaders.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Whether a header, or a variable, holds a secret by its name.
     *
     * @param name the name
     * @return true if its values are masked
     */
    public boolean isSecret(String name) {
        if (name == null) {
            return false;
        }
        String lowerCase = name.toLowerCase(Locale.ROOT);
        return SECRET_HEADERS.contains(lowerCase) || additionalHeaders.contains(lowerCase)
                || SECRET_WORDS.stream().anyMatch(lowerCase::contains);
    }

    /**
     * A copy of the headers with the values of secret headers masked.
     *
     * @param headers the headers, or {@code null}
     * @return the copy, or {@code null}
     */
    public Headers headers(Headers headers) {
        return headers == null ? null : new Headers(headers(headers.asMap()));
    }

    /**
     * A copy of the headers with the values of secret headers masked.
     *
     * @param headers the headers by name, or {@code null}
     * @return the copy, in the same order, or {@code null}
     */
    public Map<String, List<String>> headers(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, List<String>> masked = new LinkedHashMap<>();
        headers.forEach((name, values) -> masked.put(name, isSecret(name) && values != null
                ? values.stream().map(value -> MASK).toList()
                : values));
        return masked;
    }

    /**
     * The URL for a log or a message: scheme, host, path and query parameter names, with query values and user
     * information masked and without the fragment.
     *
     * @param url the URL, or {@code null}
     * @return the masked URL, or {@code null}
     */
    public static String url(String url) {
        if (url == null) {
            return null;
        }
        String withoutFragment = url.indexOf('#') < 0 ? url : url.substring(0, url.indexOf('#'));
        int query = withoutFragment.indexOf('?');
        String base = query < 0 ? withoutFragment : withoutFragment.substring(0, query);
        int authority = base.indexOf("://");
        if (authority >= 0) {
            int pathStart = base.indexOf('/', authority + 3);
            int authorityEnd = pathStart < 0 ? base.length() : pathStart;
            int userInfo = base.lastIndexOf('@', authorityEnd - 1);
            if (userInfo > authority) {
                base = base.substring(0, authority + 3) + MASK + base.substring(userInfo);
            }
        }
        if (query < 0) {
            return base;
        }
        String parameters = Arrays.stream(withoutFragment.substring(query + 1).split("&", -1))
                .map(parameter -> parameter.indexOf('=') < 0 ? parameter : parameter.substring(0, parameter.indexOf('=') + 1) + MASK)
                .collect(Collectors.joining("&"));
        return base + "?" + parameters;
    }

    /**
     * A body for a log or a message: its length and content type, never its content.
     *
     * @param body        the body, or {@code null}
     * @param contentType the content type, or {@code null}
     * @return the description
     */
    public static String body(byte[] body, String contentType) {
        String length = body == null ? "no body" : body.length + " bytes";
        return contentType == null || contentType.isBlank() ? length : length + ", " + contentType;
    }

    /**
     * A body for a log or a message: its length and the content type of its headers, never its content.
     *
     * @param body    the body, or {@code null}
     * @param headers the headers the content type is read from (any case), or {@code null}
     * @return the description
     */
    public static String body(byte[] body, Headers headers) {
        String contentType = headers == null ? null : headers.asMap().entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> entry.getValue().get(0))
                .findFirst()
                .orElse(null);
        return body(body, contentType);
    }
}
