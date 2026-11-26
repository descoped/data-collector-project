package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.error.ExecutionException;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.http.Request;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.HttpStatusRetryWhile;
import io.descoped.dc.api.security.Masking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Handler(forClass = HttpStatusRetryWhile.class)
public class HttpStatusRetryWhileHandler extends AbstractHandler<HttpStatusRetryWhile> {

    static final Logger LOG = LoggerFactory.getLogger(HttpStatusRetryWhileHandler.class);

    static String TRY_AGAIN = "TRY_AGAIN";

    public HttpStatusRetryWhileHandler(HttpStatusRetryWhile node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext context) {
        Request request = context.state(Request.class);
        Response response = context.state(Response.class);
        if (response.statusCode() == node.statusCode()) {
            try {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Retry in {} {} cause {} @ {}", node.amount(), node.duration().name().toLowerCase(), Masking.body(response.body(), response.headers()), Masking.url(request.url()));
                }
                node.duration().sleep(node.amount());
                return ExecutionContext.empty().state(TRY_AGAIN, true);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionException(e);
            }
        }
        return ExecutionContext.empty().state(TRY_AGAIN, false);
    }
}
