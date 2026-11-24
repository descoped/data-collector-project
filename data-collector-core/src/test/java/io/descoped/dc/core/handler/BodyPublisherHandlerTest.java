package io.descoped.dc.core.handler;

import com.github.mizosoft.methanol.MultipartBodyPublisher;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.node.BodyPublisher;
import io.descoped.dc.api.node.builder.BodyPublisherBuilder;
import io.descoped.dc.core.executor.Executor;
import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

import static io.descoped.dc.api.Builders.bodyPublisher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyPublisherHandlerTest {

    static Flow.Publisher<ByteBuffer> publish(BodyPublisherBuilder builder, Request.Builder requestBuilder) {
        BodyPublisher node = builder.build(null);
        ExecutionContext output = Executor.execute(node, ExecutionContext.empty().state(Request.Builder.class, requestBuilder));
        return output.state(BodyPublisher.BODY_PUBLISHER_RESULT);
    }

    static Request request(Request.Builder requestBuilder) {
        return requestBuilder.url("http://localhost/").GET().build();
    }

    @Test
    void thatPlainTextIsPublished() {
        Request.Builder requestBuilder = Request.newRequestBuilder();

        Flow.Publisher<ByteBuffer> publisher = publish(bodyPublisher().plainText("hello"), requestBuilder);

        assertEquals(5, ((HttpRequest.BodyPublisher) publisher).contentLength());
        assertTrue(request(requestBuilder).headers().firstValue("Content-Type").isEmpty());
    }

    @Test
    void thatUrlEncodedDataIsPublishedWithItsContentType() {
        Request.Builder requestBuilder = Request.newRequestBuilder();

        Flow.Publisher<ByteBuffer> publisher = publish(bodyPublisher().urlEncoded("a=b&c=d"), requestBuilder);

        assertEquals(7, ((HttpRequest.BodyPublisher) publisher).contentLength());
        assertEquals("application/x-www-form-urlencoded", request(requestBuilder).headers().firstValue("Content-Type").orElseThrow());
    }

    @Test
    void thatPartsArePublishedAsMultipartWithTheBoundary() {
        Request.Builder requestBuilder = Request.newRequestBuilder();

        Flow.Publisher<ByteBuffer> publisher = publish(bodyPublisher()
                .textPart("name", "value")
                .formPart("bytes", "bytes.txt", "data".getBytes())
                .formPart("text", "text.txt", "data"), requestBuilder);

        MultipartBodyPublisher multipart = assertInstanceOf(MultipartBodyPublisher.class, publisher);
        assertEquals(3, multipart.parts().size());
        assertEquals("multipart/form-data;boundary=" + multipart.boundary(), request(requestBuilder).headers().firstValue("Content-Type").orElseThrow());
    }

    @Test
    void thatNothingIsPublishedWithoutARequestBuilder() {
        BodyPublisher node = bodyPublisher().plainText("hello").build(null);

        ExecutionContext output = Executor.execute(node, ExecutionContext.empty());

        assertNull(output.state(BodyPublisher.BODY_PUBLISHER_RESULT));
    }
}
