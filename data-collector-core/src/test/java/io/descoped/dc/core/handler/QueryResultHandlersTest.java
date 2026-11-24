package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Headers;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Base;
import io.descoped.dc.api.node.builder.XmlTokenBuilder;
import io.descoped.dc.core.executor.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static io.descoped.dc.api.Builders.body;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.jsonToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * The body, JSON token and XML token queries run as nodes give the response body, or the file it was written to.
 */
class QueryResultHandlersTest {

    static Response response(byte[] body, BodyHandler<Path> bodyHandler) {
        return new Response() {
            @Override
            public String url() {
                return "http://localhost/";
            }

            @Override
            public Headers headers() {
                return new Headers();
            }

            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public byte[] body() {
                return body;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <R> Optional<BodyHandler<R>> bodyHandler() {
                return Optional.ofNullable((BodyHandler<R>) bodyHandler);
            }

            @Override
            public Optional<Response> previousResponse() {
                return Optional.empty();
            }
        };
    }

    static BodyHandler<Path> fileBodyHandler(Path file) throws IOException {
        return new FileBodyHandler(Files.createFile(file));
    }

    static Object result(Base node, Response response) {
        ExecutionContext output = Executor.execute(node, ExecutionContext.empty().state(Response.class, response));
        QueryResult<?> queryResult = output.state(QueryResult.class);
        return queryResult.getResult();
    }

    @Test
    void thatBodyQueryGivesTheBodyAsText() {
        assertEquals("body", result(body().build(), response("body".getBytes(StandardCharsets.UTF_8), null)));
    }

    @Test
    void thatJsonTokenQueryGivesTheBodyFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("body.json");
        assertEquals(file, result(jsonToken().build(), response(new byte[0], fileBodyHandler(file))));
    }

    @Test
    void thatXmlTokenQueryGivesTheBodyFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("body.xml");
        assertEquals(file, result(new XmlTokenBuilder().build(), response(new byte[0], fileBodyHandler(file))));
    }

    @Test
    void thatHandlerOfANodeMustBeANodeHandler() {
        Base node = get("page").url("http://localhost/").build();

        assertThrows(IllegalStateException.class, () -> new AbstractHandler<>(node) {
            @Override
            public ExecutionContext execute(ExecutionContext input) {
                return input;
            }
        });
    }
}
