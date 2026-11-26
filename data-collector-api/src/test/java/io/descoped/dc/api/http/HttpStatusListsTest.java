package io.descoped.dc.api.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpStatusListsTest {

    @Test
    void thatTheCodeListsCanBeRead() {
        assertTrue(HttpStatus.okCodes().contains(HttpStatus.valueOf(200)));
        assertTrue(HttpStatus.redirectionCodes().contains(HttpStatus.valueOf(301)));
        assertTrue(HttpStatus.clientErrorList().contains(HttpStatus.valueOf(404)));
        assertTrue(List.of(HttpStatus.clientErrorCodes()).contains(404));
        assertTrue(HttpStatus.serverErrorCodes().contains(HttpStatus.valueOf(500)));
        assertEquals(HttpStatus.okCodes(), HttpStatus.okCodes());
    }
}
