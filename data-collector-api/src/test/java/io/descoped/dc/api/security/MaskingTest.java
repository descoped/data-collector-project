package io.descoped.dc.api.security;

import io.descoped.dc.api.ConfigurationMap;
import io.descoped.dc.api.http.Headers;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaskingTest {

    @Test
    void thatTheDefaultSecretHeadersAreMasked() {
        Masking masking = Masking.defaults();
        for (String name : List.of("Authorization", "proxy-authorization", "Cookie", "Set-Cookie", "x-api-key",
                "X-Auth-Token", "client_secret", "X-Signature", "password")) {
            assertTrue(masking.isSecret(name), name);
        }
        for (String name : List.of("Accept", "Content-Type", "x-correlation-id", "User-Agent")) {
            assertFalse(masking.isSecret(name), name);
        }
        assertFalse(masking.isSecret(null));
    }

    @Test
    void thatConfiguredHeadersAreAdded() {
        Masking masking = Masking.of(new ConfigurationMap(new LinkedHashMap<>(Map.of(Masking.SECRET_HEADERS_KEY, " X-Partner-Id , SSB-Id "))));
        assertTrue(masking.isSecret("x-partner-id"));
        assertTrue(masking.isSecret("SSB-ID"));
        assertTrue(masking.isSecret("Authorization"));
        assertFalse(masking.isSecret("Accept"));
        assertFalse(Masking.of((ConfigurationMap) null).isSecret("x-partner-id"));
    }

    @Test
    void thatHeadersKeepTheirNamesAndOrder() {
        Headers headers = new Headers();
        headers.put("Accept", "application/json");
        headers.put("Authorization", "Bearer abc");
        headers.put("Cookie", "a=1");
        headers.put("Cookie", "b=2");
        Headers masked = Masking.defaults().headers(headers);
        assertEquals(List.of("Accept", "Authorization", "Cookie"), List.copyOf(masked.asMap().keySet()));
        assertEquals(List.of("application/json"), masked.allValues("Accept"));
        assertEquals(List.of(Masking.MASK), masked.allValues("Authorization"));
        assertEquals(List.of(Masking.MASK, Masking.MASK), masked.allValues("Cookie"));
        assertEquals(List.of("Bearer abc"), headers.allValues("Authorization"), "the original is unchanged");
        assertNull(Masking.defaults().headers((Headers) null));
    }

    @Test
    void thatUrlsKeepParameterNamesOnly() {
        assertEquals("https://api.example.org/v1/items", Masking.url("https://api.example.org/v1/items"));
        assertEquals("https://api.example.org/v1/items?apikey=****&page=****&flag",
                Masking.url("https://api.example.org/v1/items?apikey=s3cret&page=2&flag#token=abc"));
        assertEquals("https://****@api.example.org/v1", Masking.url("https://user:pass@api.example.org/v1"));
        assertEquals("https://api.example.org/a@b", Masking.url("https://api.example.org/a@b"));
        assertEquals("/relative?q=****", Masking.url("/relative?q=x"));
        assertNull(Masking.url(null));
    }

    @Test
    void thatBodiesAreDescribedNotShown() {
        byte[] body = "{\"access_token\":\"abc\"}".getBytes(StandardCharsets.UTF_8);
        assertEquals("22 bytes", Masking.body(body, (String) null));
        Headers headers = new Headers();
        headers.put("content-type", "application/json");
        assertEquals("22 bytes, application/json", Masking.body(body, headers));
        assertEquals("no body", Masking.body(null, (Headers) null));
    }
}
