package io.descoped.dc.api.node.builder;

import org.junit.jupiter.api.Test;

import static io.descoped.dc.api.Builders.claims;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.headerClaims;
import static io.descoped.dc.api.Builders.jwt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BuilderEqualityTest {

    @Test
    void thatOperationBuildersCompareTheirUrl() {
        GetBuilder builder = get("id").url("http://a");
        assertEquals(builder, builder);
        assertEquals(get("id").url("http://a"), builder);
        assertEquals(get("id").url("http://a").hashCode(), builder.hashCode());
        assertNotEquals(get("id").url("http://b"), builder);
        assertNotEquals(get("other").url("http://a"), builder);
        assertNotEquals(null, builder);
        assertNotEquals(new PostBuilder("id").url("http://a"), builder);
    }

    @Test
    void thatIdentityBuildersCompareTheirId() {
        IdentityBuilder builder = jwt("id", headerClaims().alg("RS256"), claims().issuer("issuer"));
        assertEquals(builder, builder);
        assertEquals(jwt("id", headerClaims().alg("RS256"), claims().issuer("issuer")), builder);
        assertEquals(jwt("id", headerClaims().alg("RS256"), claims().issuer("issuer")).hashCode(), builder.hashCode());
        assertNotEquals(jwt("other", headerClaims().alg("RS256"), claims().issuer("issuer")), builder);
        assertNotEquals(null, builder);
        assertNotEquals(new StringBodyPublisherProducerBuilder("id"), builder);
    }

    @Test
    void thatStringBodyBuildersCompareTheirData() {
        StringBodyPublisherProducerBuilder builder = new StringBodyPublisherProducerBuilder("a");
        assertEquals(builder, builder);
        assertEquals(new StringBodyPublisherProducerBuilder("a"), builder);
        assertEquals(new StringBodyPublisherProducerBuilder("a").hashCode(), builder.hashCode());
        assertNotEquals(new StringBodyPublisherProducerBuilder("b"), builder);
        assertNotEquals(null, builder);
        assertNotEquals(new JsonTokenBuilder(), builder);
    }
}
