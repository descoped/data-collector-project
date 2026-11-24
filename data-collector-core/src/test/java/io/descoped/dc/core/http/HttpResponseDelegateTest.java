package io.descoped.dc.core.http;

import io.descoped.dc.api.http.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpResponseDelegateTest {

    @Test
    void thatResponseWithoutDelegateIsNotAcceptable() {
        Response response = Response.newResponseBuilder().build();

        assertEquals(406, response.statusCode());
        assertEquals("", response.url());
        assertEquals(0, response.body().length);
        assertTrue(response.previousResponse().isEmpty());
    }
}
