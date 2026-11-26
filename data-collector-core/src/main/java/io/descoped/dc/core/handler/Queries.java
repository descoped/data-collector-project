package io.descoped.dc.core.handler;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.handler.DocumentParserFeature;
import io.descoped.dc.api.handler.QueryFeature;
import io.descoped.dc.api.handler.QueryResult;
import io.descoped.dc.api.handler.QueryState;
import io.descoped.dc.api.node.Query;
import io.descoped.dc.core.executor.Executor;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Entry points for evaluating queries and for the document parsers of query types.
 */
public class Queries {

    private Queries() {
    }

    /**
     * The document parser registered for a query type.
     *
     * @param queryClass the query type, or a class implementing it as its first interface
     * @return the parser
     * @throws IllegalStateException if no parser is registered for the query type
     */
    @SuppressWarnings("unchecked") // a query node's first interface is its query type
    public static DocumentParserFeature parserFor(Class<? extends Query> queryClass) {
        if (!queryClass.isInterface()) {
            queryClass = (Class<? extends Query>) queryClass.getInterfaces()[0];
        }
        DocumentParserFeature parser = Handlers.createSupportHandlerFor(queryClass, DocumentParserFeature.class);
        return new DocumentParserWrapper(parser);
    }

    /**
     * A query evaluator of a query, with an empty context.
     *
     * @param query the query
     * @return the evaluator, which executes the query's handler on each evaluation
     */
    public static QueryFeature from(Query query) {
        return new QueryHandlerWrapper(query);
    }

    /**
     * A query evaluator of a query, with a copy of a context.
     *
     * @param context the context, copied
     * @param query   the query
     * @return the evaluator, which executes the query's handler on each evaluation
     */
    public static QueryFeature from(ExecutionContext context, Query query) {
        return new QueryHandlerWrapper(context, query);
    }

    static class DocumentParserWrapper implements DocumentParserFeature {

        private final DocumentParserFeature parser;

        DocumentParserWrapper(DocumentParserFeature parser) {
            this.parser = parser;
        }

        @Override
        public byte[] serialize(Object document) {
            return parser.serialize(document);
        }

        @Override
        public Object deserialize(byte[] source) {
            return parser.deserialize(source);
        }

        @Override
        public void tokenDeserializer(InputStream source, Consumer<Object> entryCallback) {
            parser.tokenDeserializer(source, entryCallback);
        }
    }

    static class QueryHandlerWrapper implements QueryFeature {

        private final ExecutionContext context;
        private final Query query;

        QueryHandlerWrapper(Query query) {
            this.context = ExecutionContext.empty();
            this.query = query;
        }

        QueryHandlerWrapper(ExecutionContext context, Query query) {
            this.context = ExecutionContext.of(context);
            this.query = query;
        }

        @Override
        public List<?> evaluateList(Object data) {
            context.state(QueryState.class, new QueryState<>(QueryFeature.Type.LIST, data));
            ExecutionContext output = Executor.execute(query, context);
            @SuppressWarnings("unchecked") // the state is kept by its raw class
            QueryResult<List<?>> state = output.state(QueryResult.class);
            return state.getResult();
        }

        @Override
        public Object evaluateObject(Object data) {
            context.state(QueryState.class, new QueryState<>(Type.OBJECT, data));
            ExecutionContext output = Executor.execute(query, context);
            @SuppressWarnings("unchecked") // the state is kept by its raw class
            QueryResult<Object> state = output.state(QueryResult.class);
            return state.getResult();
        }

        @Override
        public String evaluateStringLiteral(Object data) {
            context.state(QueryState.class, new QueryState<>(Type.STRING_LITERAL, data));
            ExecutionContext output = Executor.execute(query, context);
            @SuppressWarnings("unchecked") // the state is kept by its raw class
            QueryResult<String> state = output.state(QueryResult.class);
            return state.getResult();
        }
    }
}
