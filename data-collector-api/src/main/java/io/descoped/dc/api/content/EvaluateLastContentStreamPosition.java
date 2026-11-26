package io.descoped.dc.api.content;

import io.descoped.dc.api.context.ExecutionContext;
import io.descoped.dc.api.el.EvaluationException;
import io.descoped.dc.api.health.HealthResourceUtils;

/**
 * Reads the last position of the specification's topic from the content store, and updates the health monitor
 * with it.
 */
public class EvaluateLastContentStreamPosition {

    private final ExecutionContext context;

    /**
     * Creates the evaluator.
     *
     * @param context the context, with the topic in its state ({@code global.topic}) and the content store in its
     *                services
     */
    public EvaluateLastContentStreamPosition(ExecutionContext context) {
        this.context = context;
    }

    /**
     * The last position of the topic's page topic ({@code <topic>-pages}).
     *
     * @return the position, or {@code null} if the topic has none
     * @throws io.descoped.dc.api.el.EvaluationException if the topic is not set or the content store is absent or closed
     */
    public String getLastPagePosition() {
        String topic = context.state("global.topic");
        checkIfTopicIsNull(topic);
        return getLastPositionForTopic(topic + "-pages");
    }

    /**
     * The last position of the topic.
     *
     * @return the position, or {@code null} if the topic has none
     * @throws io.descoped.dc.api.el.EvaluationException if the topic is not set or the content store is absent or closed
     */
    public String getLastPosition() {
        String topic = context.state("global.topic");
        checkIfTopicIsNull(topic);
        return getLastPositionForTopic(topic);
    }

    private void checkIfTopicIsNull(String topic) {
        if (topic == null || "".equals(topic)) {
            throw new EvaluationException("Topic is null!");
        }

    }

    private String getLastPositionForTopic(String topic) {
        try {
            ContentStore contentStore = context.services().get(ContentStore.class);
            if (contentStore == null || contentStore.isClosed()) {
                throw new EvaluationException("ContentStore is null or closed!");
            }
            /*
             * This call will trigger content-store to seek for lastPosition the first time.
             * The GCS Provider scans all avro segments in bucket in order to resolves lastPosition, which takes unkown amount of time.
             * It is i,portant that we only invoke get lastPosition once during initialization.
             */
            String lastPosition = contentStore.lastPosition(topic);

            HealthResourceUtils.updateMonitorLastPosition(context, lastPosition);

            return lastPosition;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new EvaluationException(e);
        }
    }
}
