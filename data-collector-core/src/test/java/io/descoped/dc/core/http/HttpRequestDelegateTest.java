package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Request;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpRequestDelegateTest {

    @Test
    void thatRequestNeedsAUrl() {
        Request.Builder builder = Request.newRequestBuilder().GET();

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    void thatRequestHasItsUrlAndMethod() {
        Request request = Request.newRequestBuilder().url("http://localhost/").DELETE().build();

        assertEquals("http://localhost/", request.url());
        assertEquals(Request.Method.DELETE, request.method());
    }
}
