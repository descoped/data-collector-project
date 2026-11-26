package io.descoped.dc.core.handler;

import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.node.XmlToken;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlTokenTest {

    static final String XML = "<feed><entry><id>1</id></entry><entry><id>2</id></entry></feed>";

    @Test
    void xmlTokenParser() {
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        parser.tokenDeserializer(new ByteArrayInputStream(XML.getBytes()), entries::add);
        // 2.x: token deserialization of XML is not implemented, so a document gives no entries
        assertTrue(entries.isEmpty());
    }
}
