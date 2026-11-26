package io.descoped.dc.test.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.descoped.dc.api.util.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderEventListTest {

    private static EventListResource listResource;
    private static EventItemResource itemResource;

    @BeforeAll
    static void beforeAll() {
        listResource = new EventListResource();
        itemResource = new EventItemResource();
    }

    @Test
    void renderXmlList() {
        StringWriter output = listResource.renderTemplate("event-list-xml.ftl", listResource.getListDataModel(1, 10, -1));
        String xml = listResource.compactXml(output.toString());
        System.out.printf("%s%n", xml);
        assertEquals(10, xml.split("<entry>", -1).length - 1);
    }

    @Test
    void renderXmlItem() {
        StringWriter output = listResource.renderTemplate("event-list-item-xml.ftl", itemResource.getItemDataModel(1, true, 250, 8, 200));
        String xml = listResource.compactXml(output.toString());
        xml = AbstractResource.prettyXml(xml);
        System.out.printf("%s%n", xml);
        assertTrue(xml.contains("<id>1</id>"), xml);
    }

    @Test
    void renderJsonList() {
        StringWriter output = listResource.renderTemplate("event-list-json.ftl", listResource.getListDataModel(1, 10, -1));
        String json = output.toString();
        json = listResource.compactJson(json);
        System.out.printf("%s%n", json);
        assertEquals(10, JsonParser.createJsonParser().fromJson(json, ArrayNode.class).size());
    }

    @Test
    void renderJsonItem() {
        StringWriter output = listResource.renderTemplate("event-list-item-json.ftl", itemResource.getItemDataModel(1, true, 250, 8, 200));
        String json = output.toString();
        json = listResource.compactJson(json);
        System.out.printf("%s%n", json);
        assertEquals("1", JsonParser.createJsonParser().fromJson(json, JsonNode.class).get("id").asText());
    }
}
