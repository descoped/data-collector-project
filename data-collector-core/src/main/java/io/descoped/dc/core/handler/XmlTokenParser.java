package io.descoped.dc.core.handler;

import io.descoped.dc.api.security.Masking;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.QueryException;
import io.descoped.dc.api.handler.SupportHandler;
import io.descoped.dc.api.node.XmlToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The document parser of {@code XmlToken} queries: serializes and deserializes XML documents without DTDs or external
 * entities, namespace-unaware, and reads each child element of the root as a document of its own, one at a time.
 */
@SupportHandler(forClass = XmlToken.class, selectorClass = DocumentParserFeature.class)
public class XmlTokenParser implements DocumentParserFeature {

    /**
     * Creates a parser.
     */
    public XmlTokenParser() {
        // stateless
    }

    /**
     * @param document ObjectNode (document, element or node)
     * @return byte array
     */
    @Override
    public byte[] serialize(Object document) {
        try (StringWriter writer = new StringWriter()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // Disable external DTDs and stylesheets
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource((Node) document), new StreamResult(writer));
            return writer.toString().getBytes();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new QueryException(e);
        }
    }

    static XMLReader createSAXFactory() {
        try {
            SAXParserFactory sax = SAXParserFactory.newInstance();
            // Disable external entities
            sax.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            sax.setNamespaceAware(false);
            return sax.newSAXParser().getXMLReader();
        } catch (SAXException | ParserConfigurationException e) {
            throw new QueryException(e);
        }
    }

    static DocumentBuilder createDocumentBuilder() {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

            // Disable external entities
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            documentBuilderFactory.setExpandEntityReferences(false);

            documentBuilderFactory.setNamespaceAware(false);
            return documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new QueryException(e);
        }
    }

    /**
     * @param source xml byte array
     * @return Document
     */
    @Override
    public Object deserialize(byte[] source) {
        try {
            XMLReader reader = createSAXFactory();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(source)) {
                SAXSource saxSource = new SAXSource(reader, new InputSource(bais));
                DocumentBuilder documentBuilder = createDocumentBuilder();
                Document doc = documentBuilder.parse(saxSource.getInputSource());
                doc.normalizeDocument();
                return doc;
            }

        } catch (SAXException | IOException e) {
            throw new QueryException("Error parsing " + Masking.body(source, (String) null), e);
        }
    }

    @Override
    public void tokenDeserializer(InputStream source, Consumer<Object> entryCallback) {
        // each child element of the root is an entry: a document of its own, whose root is that element, so a query
        // such as /entry/id reads it as deserialize() would read the element alone (namespace-unaware, as there)
        try {
            XMLStreamReader reader = createStAXFactory().createXMLStreamReader(new BufferedInputStream(source));
            try {
                if (!toNextElement(reader)) {
                    throw new IllegalStateException("Root element NOT found!");
                }
                Map<String, String> namespaceDeclarations = namespaceDeclarations(reader);
                DocumentBuilder documentBuilder = createDocumentBuilder();
                reader.next();
                while (toNextElement(reader)) {
                    Document entry = documentBuilder.newDocument();
                    Element element = readElement(reader, entry);
                    // the root's namespace declarations go with each entry, so a prefixed entry is a document of its own
                    namespaceDeclarations.forEach((name, value) -> {
                        if (!element.hasAttribute(name)) {
                            element.setAttribute(name, value);
                        }
                    });
                    entry.appendChild(element);
                    entryCallback.accept(entry);
                    reader.next();
                }
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new QueryException("Error reading the XML tokens", e);
        }
    }

    static XMLInputFactory createStAXFactory() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // no DTDs or external entities, as for the DOM and SAX parsers
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        return factory;
    }

    // the xmlns and xmlns:* attributes of the element the reader is at (namespace-unaware, they are attributes)
    private static Map<String, String> namespaceDeclarations(XMLStreamReader reader) {
        Map<String, String> declarations = new LinkedHashMap<>();
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String name = reader.getAttributeLocalName(i);
            if ("xmlns".equals(name) || name.startsWith("xmlns:")) {
                declarations.put(name, reader.getAttributeValue(i));
            }
        }
        return declarations;
    }

    // moves to the next start element at this level; false at the end of the enclosing element or the document
    private static boolean toNextElement(XMLStreamReader reader) throws XMLStreamException {
        while (true) {
            switch (reader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    return true;
                case XMLStreamConstants.END_ELEMENT, XMLStreamConstants.END_DOCUMENT:
                    return false;
                case XMLStreamConstants.DTD:
                    // a DOCTYPE fails, as the DOM parser of xpath() refuses it
                    throw new XMLStreamException("DOCTYPE is disallowed", reader.getLocation());
                default:
                    reader.next();
            }
        }
    }

    // reads the element the reader is at, with its subtree; the reader is left at its end element
    private static Element readElement(XMLStreamReader reader, Document document) throws XMLStreamException {
        Element element = document.createElement(reader.getLocalName());
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            element.setAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
        }
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    element.appendChild(readElement(reader, document));
                    break;
                case XMLStreamConstants.CHARACTERS, XMLStreamConstants.SPACE:
                    element.appendChild(document.createTextNode(reader.getText()));
                    break;
                case XMLStreamConstants.CDATA:
                    element.appendChild(document.createCDATASection(reader.getText()));
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    return element;
                default:
                    break;
            }
        }
        throw new IllegalStateException("Element not closed: " + element.getTagName());
    }
}
