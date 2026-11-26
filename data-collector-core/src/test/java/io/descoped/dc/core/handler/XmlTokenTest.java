package io.descoped.dc.core.handler;

import io.descoped.dc.api.Builders;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.node.XmlToken;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlTokenTest {

    static final String XML = "<feed><entry><id>1</id></entry><entry><id>2</id></entry></feed>";

    @Test
    void xmlTokenParser() {
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        parser.tokenDeserializer(new ByteArrayInputStream(XML.getBytes()), entries::add);
        // each child element of the root is an entry, read as a document of its own
        assertEquals(2, entries.size());
        QueryFeature id = Queries.from(Builders.xpath("/entry/id").build());
        assertEquals("1", id.evaluateStringLiteral(entries.get(0)));
        assertEquals("2", id.evaluateStringLiteral(entries.get(1)));
    }

    @Test
    void thatNamespacesAttributesAndTextAreKept() {
        String xml = """
                <?xml version='1.0' encoding='UTF-8'?>
                <hendelser xmlns="urn:no:example">
                  <hendelse type="a"><sekvensnummer>41</sekvensnummer><tekst><![CDATA[x < y]]></tekst></hendelse>
                  <hendelse type="b"><sekvensnummer>42</sekvensnummer></hendelse>
                </hendelser>""";
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        parser.tokenDeserializer(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), entries::add);
        assertEquals(2, entries.size());
        assertEquals("41", Queries.from(Builders.xpath("/hendelse/sekvensnummer").build()).evaluateStringLiteral(entries.get(0)));
        assertEquals("b", Queries.from(Builders.xpath("/hendelse/@type").build()).evaluateStringLiteral(entries.get(1)));
        assertEquals("x < y", Queries.from(Builders.xpath("/hendelse/tekst").build()).evaluateStringLiteral(entries.get(0)));
        String serialized = new String(parser.serialize(entries.get(1)), StandardCharsets.UTF_8);
        // the entry carries the root's namespace declaration
        assertTrue(serialized.contains("<hendelse xmlns=\"urn:no:example\" type=\"b\"><sekvensnummer>42</sekvensnummer></hendelse>"), serialized);
    }

    // namespace-unaware, as deserialize(): a prefixed element or attribute keeps its prefix in its name, and the entry
    // carries the root's namespace declarations
    @Test
    void thatPrefixedNamesAreKeptAsDeserializeKeepsThem() {
        String xml = "<a:hendelser xmlns:a=\"urn:a\"><a:hendelse a:type=\"x\"><a:id>1</a:id></a:hendelse></a:hendelser>";
        String element = "<a:hendelse a:type=\"x\" xmlns:a=\"urn:a\"><a:id>1</a:id></a:hendelse>";
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        parser.tokenDeserializer(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), entries::add);
        assertEquals(1, entries.size());
        Object alone = parser.deserialize(element.getBytes(StandardCharsets.UTF_8));
        assertEquals(new String(parser.serialize(alone), StandardCharsets.UTF_8),
                new String(parser.serialize(entries.get(0)), StandardCharsets.UTF_8));
    }

    // a DOCTYPE fails, as deserialize() refuses it
    @Test
    void thatADoctypeFails() {
        String xml = "<!DOCTYPE feed [<!ENTITY x \"y\">]><feed><entry/></feed>";
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        byte[] source = xml.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream stream = new ByteArrayInputStream(source);
        assertThrows(RuntimeException.class, () -> parser.tokenDeserializer(stream, entries::add));
        assertThrows(RuntimeException.class, () -> parser.deserialize(source));
        assertTrue(entries.isEmpty());
    }

    @Test
    void thatAnEmptyRootGivesNoEntries() {
        DocumentParserFeature parser = Queries.parserFor(XmlToken.class);
        List<Object> entries = new ArrayList<>();
        parser.tokenDeserializer(new ByteArrayInputStream("<feed/>".getBytes(StandardCharsets.UTF_8)), entries::add);
        assertTrue(entries.isEmpty());
    }
}
