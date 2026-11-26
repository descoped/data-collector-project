package io.descoped.dc.test.client;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A response of the test client, with assertions on its status code.
 *
 * @param <T> the body's type
 */
public class ResponseHelper<T> {

    private final HttpResponse<T> response;
    private final T body;

    /**
     * Wraps a response.
     *
     * @param response the response
     */
    public ResponseHelper(HttpResponse<T> response) {
        this.response = response;
        this.body = response.body();
    }

    /**
     * The response.
     *
     * @return the response
     */
    public HttpResponse<T> response() {
        return response;
    }

    /**
     * The response's body.
     *
     * @return the body
     */
    public T body() {
        return body;
    }

    /**
     * Asserts that the status code is one of the given codes.
     *
     * @param anyOf the expected status codes
     * @return this helper
     */
    public ResponseHelper<T> expectAnyOf(int... anyOf) {
        int matchingStatusCode = -1;
        for (int statusCode : anyOf) {
            if (response.statusCode() == statusCode) {
                matchingStatusCode = statusCode;
            }
        }
        assertTrue(matchingStatusCode != -1, "Actual statusCode was " + response.statusCode() + " message: " + body);
        return this;
    }

    /**
     * Asserts the status code 400.
     *
     * @return this helper
     */
    public ResponseHelper<T> expect400BadRequest() {
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_BAD_REQUEST, String.valueOf(body));
        return this;
    }

    /**
     * Asserts the status code 404.
     *
     * @return this helper
     */
    public ResponseHelper<T> expect404NotFound() {
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_NOT_FOUND, String.valueOf(body));
        return this;
    }

    /**
     * Asserts the status code 200.
     *
     * @return this helper
     */
    public ResponseHelper<T> expect200Ok() {
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_OK, String.valueOf(body));
        return this;
    }

    /**
     * Asserts the status code 201.
     *
     * @return this helper
     */
    public ResponseHelper<T> expect201Created() {
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_CREATED, String.valueOf(body));
        return this;
    }

    /**
     * Asserts the status code 204.
     *
     * @return this helper
     */
    public ResponseHelper<T> expect204NoContent() {
        assertEquals(response.statusCode(), HttpURLConnection.HTTP_NO_CONTENT, String.valueOf(body));
        return this;
    }
}
