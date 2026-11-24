package io.descoped.dc.core.handler;

import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.node.RegEx;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.descoped.dc.api.Builders.jqpath;
import static io.descoped.dc.api.Builders.regex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegExHandlerTest {

    static final RegEx NODE = regex(jqpath(".link"), "(?<=[?&]position=)[^&]*").build();

    @Test
    void thatSubQueryResultIsMatched() {
        QueryFeature query = Queries.from(NODE);

        assertEquals("42", query.evaluateStringLiteral("{\"link\": \"http://example.com/events?position=42&pageSize=10\"}"));
    }

    // the handler matches the text the sub-query gave, as a string or as bytes
    @Test
    void thatTextAndBytesMatchAlike() {
        RegExHandler handler = new RegExHandler(NODE);
        String link = "http://example.com/events?position=42&pageSize=10";

        assertEquals("42", handler.evaluateStringLiteral(link));
        assertEquals("42", handler.evaluateStringLiteral(link.getBytes(StandardCharsets.UTF_8)));
        assertNull(handler.evaluateStringLiteral("http://example.com/events"));
    }
}
