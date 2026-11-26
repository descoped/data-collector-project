package io.descoped.dc.core.handler;

import io.descoped.dc.api.PositionObserver;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.ExpressionLanguage;
import io.descoped.dc.api.handler.Handler;
import io.descoped.dc.api.node.Publish;
import io.descoped.dc.core.executor.BufferedReordering;
import io.descoped.dc.core.executor.CommittedPositions;
import io.descoped.dc.core.health.HealthWorkerMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Executes a {@code Publish} node: when the content store holds content for the evaluated position, it marks the
 * position completed and publishes, in order, the positions that the reordering buffer releases, then reports them
 * to the position observer and the health monitor.
 */
@Handler(forClass = Publish.class)
public class PublishHandler extends AbstractNodeHandler<Publish> {

    private static final Logger LOG = LoggerFactory.getLogger(PublishHandler.class);

    /**
     * Creates the handler of a node.
     *
     * @param node the node to execute
     */
    public PublishHandler(Publish node) {
        super(node);
    }

    @Override
    public ExecutionContext execute(ExecutionContext input) {
        super.execute(input);
        ExpressionLanguage el = new ExpressionLanguage(input);

        String positionValue = (String) el.evaluateExpression(node.positionVariableExpression());

        @SuppressWarnings("unchecked") // the service is kept by its raw class
        BufferedReordering<String> bufferedReordering = input.services().get(BufferedReordering.class);

        ContentStore contentStore = input.services().get(ContentStore.class);
        String topicName = node.configurations().flowContext().topic();
        if (topicName == null) {
            throw new IllegalStateException("Unable to resolve topic!");
        }
        Set<String> contentKeys = contentStore.contentKeys(topicName, positionValue);

        HealthWorkerMonitor monitor = input.services().get(HealthWorkerMonitor.class);
        CommittedPositions committedPositions = input.services().contains(CommittedPositions.class)
                ? input.services().get(CommittedPositions.class) : null;

        if (!contentKeys.isEmpty()) {
            contentStore.lock(topicName);
            try {
                bufferedReordering.addCompleted(positionValue, orderedPositions -> {
                    if (monitor != null && monitor.contentStream().hasNotSetStartPosition()) {
                        monitor.contentStream().setStartPosition(orderedPositions.get(0));
                    }

                    // a position an earlier run committed is completed without being written again (#71)
                    List<String> toPublish = new ArrayList<>(orderedPositions.size());
                    List<String> committed = new ArrayList<>();
                    for (String orderedPosition : orderedPositions) {
                        (committedPositions != null && committedPositions.take(orderedPosition) ? committed : toPublish).add(orderedPosition);
                    }
                    if (!committed.isEmpty()) {
                        contentStore.discard(topicName, committed.toArray(new String[0]));
                    }
                    if (!toPublish.isEmpty()) {
                        contentStore.publish(topicName, toPublish.toArray(new String[0]));
                    }

                    if (LOG.isInfoEnabled()) {
                        LOG.info("Reordered sequence: [{}] with content [{}]",
                                String.join(",", orderedPositions),
                                String.join(",", contentKeys)
                        );
                    }
                    PositionObserver positionObserver = input.state(PositionObserver.class);
                    // only avail in scope if coming from paginate
                    if (positionObserver != null) {
                        positionObserver.completed(orderedPositions.size());
                    }

                    if (monitor != null) {
                        monitor.contentStream().setLastPosition(orderedPositions.get(orderedPositions.size() - 1));
                    }
                });
            } finally {
                contentStore.unlock(topicName);
            }
        }

        return ExecutionContext.empty();
    }

}
