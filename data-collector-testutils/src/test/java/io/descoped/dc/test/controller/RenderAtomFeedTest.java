package io.descoped.dc.test.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderAtomFeedTest {

    private static AtomFeedResource atomFeedResource;

    @BeforeAll
    static void beforeAll() {
        atomFeedResource = new AtomFeedResource();
    }

    @Test
    void renderXmlFeed() {
        String output = atomFeedResource.renderAtomFeedAsXml("1000", 25, "-1");
        String xml = atomFeedResource.compactXml(output);
        xml = atomFeedResource.prettyXml(xml);
        System.out.printf("%s%n", xml);
        assertEquals(25, xml.split("<entry", -1).length - 1);
    }

}
