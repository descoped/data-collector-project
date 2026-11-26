package io.descoped.dc.api.http;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// TODO https://en.wikipedia.org/wiki/List_of_HTTP_status_codes
/**
 * The HTTP status codes the data collector knows, with their reason phrases.
 */
public enum HttpStatus {

    /* 2XX: generally "OK" */

    /**
     * 200 OK.
     */
    HTTP_OK(200, "OK"),
    /**
     * 201 Created.
     */
    HTTP_CREATED(201, "Created"),
    /**
     * 202 Accepted.
     */
    HTTP_ACCEPTED(202, "Accepted"),
    /**
     * 203 Non-Authoritative Information.
     */
    HTTP_NOT_AUTHORITATIVE(203, "Non-Authoritative Information"),
    /**
     * 204 No Content.
     */
    HTTP_NO_CONTENT(204, "No Content"),
    /**
     * 205 Reset Content.
     */
    HTTP_RESET(205, "Reset Content"),
    /**
     * 206 Partial Content.
     */
    HTTP_PARTIAL(206, "Partial Content"),

    /* 3XX: relocation/redirect */

    /**
     * 300 Multiple Choices.
     */
    HTTP_MULT_CHOICE(300, "Multiple Choices"),
    /**
     * 301 Moved Permanently.
     */
    HTTP_MOVED_PERM(301, "Moved Permanently"),
    /**
     * 302 Temporary Redirect.
     */
    HTTP_MOVED_TEMP(302, "Temporary Redirect"),
    /**
     * 303 See Other.
     */
    HTTP_SEE_OTHER(303, "See Other"),
    /**
     * 304 Not Modified.
     */
    HTTP_NOT_MODIFIED(304, "Not Modified"),
    /**
     * 305 Use Proxy.
     */
    HTTP_USE_PROXY(305, "Use Proxy"),

    /* 4XX: client error */

    /**
     * 400 Bad Request.
     */
    HTTP_BAD_REQUEST(400, "Bad Request"),
    /**
     * 401 Unauthorized.
     */
    HTTP_UNAUTHORIZED(401, "Unauthorized"),
    /**
     * 402 Payment Required.
     */
    HTTP_PAYMENT_REQUIRED(402, "Payment Required"),
    /**
     * 403 Forbidden.
     */
    HTTP_FORBIDDEN(403, "Forbidden"),
    /**
     * 404 Not Found.
     */
    HTTP_NOT_FOUND(404, "Not Found"),
    /**
     * 405 Method Not Allowed.
     */
    HTTP_BAD_METHOD(405, "Method Not Allowed"),
    /**
     * 406 Not Acceptable.
     */
    HTTP_NOT_ACCEPTABLE(406, "Not Acceptable"),
    /**
     * 407 Proxy Authentication Required.
     */
    HTTP_PROXY_AUTH(407, "Proxy Authentication Required"),
    /**
     * 408 Request Timeout.
     */
    HTTP_CLIENT_TIMEOUT(408, "Request Timeout"),
    /**
     * 409 Conflict.
     */
    HTTP_CONFLICT(409, "Conflict"),
    /**
     * 410 Gone.
     */
    HTTP_GONE(410, "Gone"),
    /**
     * 411 Length Required.
     */
    HTTP_LENGTH_REQUIRED(411, "Length Required"),
    /**
     * 412 Precondition Failed.
     */
    HTTP_PRECON_FAILED(412, "Precondition Failed"),
    /**
     * 413 Request Entity Too Large.
     */
    HTTP_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    /**
     * 414 Request-URI Too Large.
     */
    HTTP_REQ_TOO_LONG(414, "Request-URI Too Large"),
    /**
     * 415 Unsupported Media Type.
     */
    HTTP_UNSUPPORTED_TYPE(415, "Unsupported Media Type"),

    /* 5XX: server error */

    /**
     * 500 Internal Server Error.
     */
    HTTP_INTERNAL_ERROR(500, "Internal Server Error"),
    /**
     * 501 Not Implemented.
     */
    HTTP_NOT_IMPLEMENTED(501, "Not Implemented"),
    /**
     * 502 Bad Gateway.
     */
    HTTP_BAD_GATEWAY(502, "Bad Gateway"),
    /**
     * 503 Service Unavailable.
     */
    HTTP_UNAVAILABLE(503, "Service Unavailable"),
    /**
     * 504 Gateway Timeout.
     */
    HTTP_GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    /**
     * 505 HTTP Version Not Supported.
     */
    HTTP_VERSION(505, "HTTP Version Not Supported");

    private static final Map<Integer, HttpStatus> okMap = new LinkedHashMap<>();
    private static final Map<Integer, HttpStatus> redirectionMap = new LinkedHashMap<>();
    private static final Map<Integer, HttpStatus> clientErrorMap = new LinkedHashMap<>();
    private static final Map<Integer, HttpStatus> serverErrorMap = new LinkedHashMap<>();

    private final int statusCode;
    private final String reason;

    HttpStatus(int statusCode, String reason) {
        this.statusCode = statusCode;
        this.reason = reason;
    }

    static Map<Integer, HttpStatus> rangeMap(int fromStatusCodeInclusive, int toStatusCodeInclusive) {
        Map<Integer, HttpStatus> rangeMap = new LinkedHashMap<>();
        for (HttpStatus value : values()) {
            if (value.statusCode >= fromStatusCodeInclusive && value.statusCode <= toStatusCodeInclusive) {
                rangeMap.put(value.statusCode, value);
            }
        }
        return rangeMap;
    }

    static void buildOkMap() {
        if (okMap.isEmpty()) {
            okMap.putAll(rangeMap(200, 299));
        }
    }

    static void buildRedirectionMap() {
        if (redirectionMap.isEmpty()) {
            redirectionMap.putAll(rangeMap(300, 399));
        }
    }

    static void buildClientErrorMap() {
        if (clientErrorMap.isEmpty()) {
            clientErrorMap.putAll(rangeMap(400, 499));
        }
    }

    static void buildServerErrorMap() {
        if (serverErrorMap.isEmpty()) {
            serverErrorMap.putAll(rangeMap(500, 599));
        }
    }

    /**
     * The known 2xx status codes.
     *
     * @return the status codes
     */
    public static List<HttpStatus> okCodes() {
        buildOkMap();
        return List.copyOf(okMap.values());
    }

    /**
     * Whether a status code is a known 2xx code.
     *
     * @param statusCode the status code
     * @return {@code true} if it is
     */
    public static boolean isOk(int statusCode) {
        buildOkMap();
        return okMap.containsKey(statusCode);
    }

    /**
     * The known 3xx status codes.
     *
     * @return the status codes
     */
    public static List<HttpStatus> redirectionCodes() {
        buildRedirectionMap();
        return List.copyOf(redirectionMap.values());
    }

    /**
     * Whether a status code is a known 3xx code.
     *
     * @param statusCode the status code
     * @return {@code true} if it is
     */
    public static boolean isRedirection(int statusCode) {
        buildRedirectionMap();
        return redirectionMap.containsKey(statusCode);
    }

    /**
     * The known 4xx status codes.
     *
     * @return the status codes
     */
    public static List<HttpStatus> clientErrorList() {
        buildClientErrorMap();
        return List.copyOf(clientErrorMap.values());
    }

    /**
     * The known 4xx status codes, as numbers.
     *
     * @return the status codes
     */
    public static Integer[] clientErrorCodes() {
        return clientErrorList().stream().map(code -> code.statusCode).toArray(Integer[]::new);
    }

    /**
     * Whether a status code is a known 4xx code.
     *
     * @param statusCode the status code
     * @return {@code true} if it is
     */
    public static boolean isClientError(int statusCode) {
        buildClientErrorMap();
        return clientErrorMap.containsKey(statusCode);
    }

    /**
     * The known 5xx status codes.
     *
     * @return the status codes
     */
    public static List<HttpStatus> serverErrorCodes() {
        buildServerErrorMap();
        return List.copyOf(serverErrorMap.values());
    }

    /**
     * Whether a status code is a known 5xx code.
     *
     * @param statusCode the status code
     * @return {@code true} if it is
     */
    public static boolean isServerError(int statusCode) {
        buildServerErrorMap();
        return serverErrorMap.containsKey(statusCode);
    }

    /**
     * The known status codes in a range.
     *
     * @param fromStatusCodeInclusive the lowest code
     * @param toStatusCodeInclusive   the highest code
     * @return the status codes
     */
    public static List<HttpStatus> range(int fromStatusCodeInclusive, int toStatusCodeInclusive) {
        return new ArrayList<>(rangeMap(fromStatusCodeInclusive, toStatusCodeInclusive).values());
    }

    /**
     * The status of a code.
     *
     * @param statusCode the code
     * @return the status
     * @throws IllegalStateException if the code is not known
     */
    public static HttpStatus valueOf(int statusCode) {
        for (HttpStatus value : values()) {
            if (value.statusCode == statusCode) {
                return value;
            }
        }
        throw new IllegalStateException("HttpStatusCode is not supported: " + statusCode);
    }

    /**
     * The status code.
     *
     * @return the code
     */
    public int code() {
        return statusCode;
    }

    /**
     * The reason phrase.
     *
     * @return the reason phrase
     */
    public String reason() {
        return reason;
    }


}
