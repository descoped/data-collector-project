package io.descoped.dc.api.node.builder;

import io.descoped.dc.api.node.Node;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * TODO imlement a specialized flow language (tbd) that expresses the flow in a human readable computer sequence
 */
class PrintableExecutionPlan {

    private static final String VARIABLE_TO = "%s to %s";
    private static final String BIND = " bind [%s]";

    private PrintableExecutionPlan() {
    }

    static String build(Node flowNode) {
        StringBuilder builder = new StringBuilder();
        NodeBuilder.FlowNode.depthFirstPreOrderFullTraversal(0, new LinkedHashSet<>(), new LinkedList<>(), flowNode, (ancestors, visitNode) -> {
            String indent = Arrays.stream(new String[ancestors.size()]).map(element -> " ").collect(Collectors.joining());

            AtomicBoolean handled = new AtomicBoolean();

            visitNode.given(GetBuilder.GetNode.class, handled, node -> {
                builder.append(String.format("%s(%s) get(%s) => %s%n", indent, ancestors.size(),
                        node.id(),
                        node.url())
                );
            });

            visitNode.given(PostBuilder.PostNode.class, handled, node -> {
                builder.append(String.format("%s(%s) post(%s) => %s%n", indent, ancestors.size(),
                        node.id(),
                        node.url())
                );
            });

            visitNode.given(PutBuilder.PutNode.class, handled, node -> {
                builder.append(String.format("%s(%s) put(%s) => %s%n", indent, ancestors.size(),
                        node.id(),
                        node.url())
                );
            });

            visitNode.given(DeleteBuilder.DeleteNode.class, handled, node -> {
                builder.append(String.format("%s(%s) delete(%s) => %s%n", indent, ancestors.size(),
                        node.id(),
                        node.url())
                );
            });

            visitNode.given(ProcessBuilder.ProcessNode.class, handled, node -> {
                builder.append(String.format("%s(%s) process(Class<%s>) produces required-variables: %s%n", indent, ancestors.size(),
                        node.processorClass().getSimpleName(),
                        node.requiredOutputs())
                );
            });

            visitNode.given(ExecuteBuilder.ExecuteNode.class, handled, node -> appendExecute(builder, indent, ancestors.size(), node));

            visitNode.given(PaginateBuilder.PaginateNode.class, handled, node -> appendPaginate(builder, indent, ancestors.size(), node));

            visitNode.given(SequenceBuilder.SequenceNode.class, handled, node -> {
                builder.append(String.format("%s(%s) sequence forEach [%s] sequenced by [%s]%n", indent, ancestors.size(),
                        node.splitNode.expression(), node.expectedNode.expression()));
            });

            visitNode.given(NextPageBuilder.NextPageNode.class, handled, node -> {
                builder.append(String.format("%s(%s) nextPage output [%s]%n", indent, ancestors.size(),
                        node.outputs().entrySet().stream().map(entry -> (entry.getKey() + "=" + entry.getValue())).collect(Collectors.joining(","))));
            });

            visitNode.given(ParallelBuilder.ParallelNode.class, handled, node -> appendParallel(builder, indent, ancestors.size(), node));

            visitNode.given(AddContentBuilder.AddContentNode.class, handled, node -> {
                builder.append(String.format("%s(%s) addContent to %s named '%s'%n", indent, ancestors.size(), node.positionVariableExpression(), node.contentKey()));
            });

            visitNode.given(PublishBuilder.PublishNode.class, handled, node -> {
                builder.append(String.format("%s(%s) publish position by %s%n", indent, ancestors.size(),
                        node.positionVariableExpression)
                );
            });

            // serialize unhandled nodes
            if (!handled.get()) {
                builder.append(String.format("%s(%s) %s%n", indent, ancestors.size(), visitNode));
            }

        });
        return builder.toString();
    }

    private static void appendExecute(StringBuilder builder, String indent, int depth, ExecuteBuilder.ExecuteNode node) {
        builder.append(String.format("%s(%s) execute(%s)", indent, depth,
                node.executeId()));

        if (!node.requiredInputs().isEmpty()) {
            builder.append(" with required-input-variables: ").append(node.requiredInputs());
        }

        if (!node.inputVariable().isEmpty()) {
            String inlineInputs = node.inputVariable().entrySet().stream().map(e -> String.format(VARIABLE_TO, e.getKey(), e.getValue().expression())).collect(Collectors.joining(", "));
            builder.append(String.format(BIND, inlineInputs));
        }

        builder.append(String.format("%n"));
    }

    private static void appendPaginate(StringBuilder builder, String indent, int depth, PaginateBuilder.PaginateNode node) {
        builder.append(String.format("%s(%s) paginate(%s)", indent, depth,
                node.id())
        );

        if (node.addPageContent) {
            builder.append(" and addPageContent");
        }

        if (!node.variables.isEmpty()) {
            String contextVariables = node.variables.entrySet().stream().map(e -> String.format(VARIABLE_TO, e.getKey(), e.getValue())).collect(Collectors.joining(", "));
            builder.append(String.format(BIND, contextVariables));
        }

        builder.append(" until ").append(node.condition().getClass().getSimpleName()).append("(").append(node.condition().identifier()).append(")");

        builder.append(String.format("%n"));
    }

    private static void appendParallel(StringBuilder builder, String indent, int depth, ParallelBuilder.ParallelNode node) {
        builder.append(String.format("%s(%s) parallel each [%s]", indent, depth,
                node.splitQueryNode.expression())
        );

        if (!node.variables.isEmpty()) {
            String contextVariables = node.variables.entrySet().stream()
                    .map(e -> String.format(VARIABLE_TO, e.getKey(), e.getValue().expression())).collect(Collectors.joining(", "));
            builder.append(String.format(BIND, contextVariables));
        }

        builder.append(String.format("%n"));
    }
}
