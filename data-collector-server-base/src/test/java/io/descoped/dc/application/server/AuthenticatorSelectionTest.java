package io.descoped.dc.application.server;

import io.descoped.dc.application.security.BearerTokenAuthenticator;
import io.descoped.dc.application.spi.Authenticator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatorSelectionTest {

    @Test
    void thatExactlyOneProviderMustHaveTheName() {
        Authenticator bearer = new BearerTokenAuthenticator();
        Authenticator other = new BearerTokenAuthenticator() {
            @Override
            public String name() {
                return "other";
            }
        };
        assertSame(bearer, UndertowApplication.select(List.of(other, bearer), "bearer"));
        assertEquals("No authentication provider named: none",
                assertThrows(IllegalStateException.class, () -> UndertowApplication.select(List.of(bearer), "none")).getMessage());
        List<Authenticator> twice = List.of(bearer, new BearerTokenAuthenticator());
        assertEquals("More than one authentication provider named: bearer",
                assertThrows(IllegalStateException.class, () -> UndertowApplication.select(twice, "bearer")).getMessage());
    }
}
