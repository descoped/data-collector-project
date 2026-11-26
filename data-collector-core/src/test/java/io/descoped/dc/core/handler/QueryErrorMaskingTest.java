package io.descoped.dc.core.handler;

import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.node.XPath;
import io.descoped.dc.api.node.XmlToken;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryErrorMaskingTest {

    static final String TOKEN = "eyJhbGciOiJSUzI1NiJ9.secret-token-value";
    static final byte[] BROKEN = ("<root><token>" + TOKEN + "</token>").getBytes(StandardCharsets.UTF_8);

    @Test
    void thatAFailedXPathParseGivesTheDocumentBySizeOnly() {
        thatAFailedParseGivesTheDocumentBySizeOnly(Queries.parserFor(XPath.class));
    }

    @Test
    void thatAFailedXmlTokenParseGivesTheDocumentBySizeOnly() {
        thatAFailedParseGivesTheDocumentBySizeOnly(Queries.parserFor(XmlToken.class));
    }

    private static void thatAFailedParseGivesTheDocumentBySizeOnly(DocumentParserFeature parser) {
        RuntimeException e = assertThrows(RuntimeException.class, () -> parser.deserialize(BROKEN));
        assertFalse(String.valueOf(e.getMessage()).contains(TOKEN), e.getMessage());
        assertTrue(String.valueOf(e.getMessage()).contains(String.valueOf(BROKEN.length)), e.getMessage());
    }
}
