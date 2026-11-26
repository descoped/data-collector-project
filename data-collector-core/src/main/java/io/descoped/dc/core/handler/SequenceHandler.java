package io.descoped.dc.core.handler;

import io.descoped.dc.api.PageContext;
import io.descoped.dc.api.PositionObserver;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.http.Response;
import io.descoped.dc.api.node.Sequence;
import io.descoped.dc.core.executor.BufferedReordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes a {@code Sequence} node: splits the response into entries (by tokens from the file the body was written
 * to, when the response has a body handler), evaluates the node's expected query on each entry to get its position,
 * and registers the positions as expected in the reordering buffer, the position observer and the page context. It
 * throws {@code EndOfStreamException} when the page has no entries.
 */
@Handler(forClass = Sequence.class)
public class SequenceHandler extends AbstractNodeHandler<Sequence> {

    private static final Logger LOG = LoggerFactory.getLogger(SequenceHandler.class);

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public SequenceHandler(Sequence node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);

        Response response = input.state(Response.class);
        @SuppressWarnings("unchecked") // the service is kept by its raw class
        BufferedReordering<String> bufferedReordering = input.services().get(BufferedReordering.class);
        PageContext.Builder pageContextBuilder = input.state(PageContext.Builder.class);

        List<String> positionList = new ArrayList<>();

        if (response.bodyHandler().isPresent()) {
            // split using sequential token deserializer
            doTokenizerQuery(input, response, positionList, bufferedReordering);

        } else {
            // split by query
            doSplitQuery(input, response, positionList, bufferedReordering);
        }

        if (positionList.isEmpty()) {
            LOG.warn("Reached end of stream! No more elements from position: {}", pageContextBuilder.nextPositionVariableNames().stream().map(name -> name + "=" + input.variable(name)).toList());
            throw new EndOfStreamException();
        }

        // only avail in scope if coming from paginate
        PositionObserver positionObserver = input.state(PositionObserver.class);
        if (positionObserver != null) {
            positionObserver.expected(positionList.size());
        }

        pageContextBuilder.expectedPositions(positionList);

        return ExecutionContext.empty();
    }

    void doTokenizerQuery(ExecutionContext input, Response response, List<String> positionList, BufferedReordering<String> bufferedReordering) {
        try {
            final QueryFeature expectedQuery = Queries.from(input, node.expectedQuery());
            final DocumentParserFeature parser = Queries.parserFor(node.splitToListQuery().getClass());
            final BodyHandler<Path> fileBodyHandler = response.<Path>bodyHandler().orElseThrow();

            try (FileInputStream fis = new FileInputStream(fileBodyHandler.body().toFile())) {
                AtomicLong counter = new AtomicLong();
                parser.tokenDeserializer(fis, entry -> {
                    String position = expectedQuery.evaluateStringLiteral(entry);
                    positionList.add(position);
                    bufferedReordering.addExpected(position);
                    if (counter.incrementAndGet() % 50000 == 0) {
                        LOG.trace("Sequence count: {}", counter.get());
                    }
                });
                LOG.info("Sequence count: {}", counter.get());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void doSplitQuery(ExecutionContext input, Response response, List<String> positionList, BufferedReordering<String> bufferedReordering) {
        final List<?> splitToListItemList = Queries.from(input, node.splitToListQuery()).evaluateList(response.body());
        final QueryFeature queryFeature = Queries.from(input, node.expectedQuery());

        for (Object item : splitToListItemList) {
            String position = queryFeature.evaluateStringLiteral(item);
            positionList.add(position);
            bufferedReordering.addExpected(position);
        }
    }

}
