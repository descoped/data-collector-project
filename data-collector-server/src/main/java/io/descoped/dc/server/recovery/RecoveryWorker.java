package io.descoped.dc.server.recovery;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.content.ContentStreamProducer;
import io.descoped.dc.api.ids.TimeOrderedIds;
import io.descoped.dc.server.content.ContentStoreComponent;
import io.descoped.dc.server.db.LmdbEnvironment;
import io.descoped.dc.server.db.SequenceDbHelper;
import io.descoped.dc.server.integrity.JsonArrayWriter;
import io.descoped.rawdata.api.RawdataTopics;
import org.lmdbjava.Dbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.descoped.dc.server.db.SequenceDbHelper.getSequenceDatabaseLocation;

/**
 * Copies a topic to another, one message per position, from an integrity check's index.
 */
public class RecoveryWorker {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryWorker.class);

    private final DynamicConfiguration configuration;
    private final ContentStore contentStore;
    private final ContentStore recoveryContentStore;
    private final RecoveryMonitor monitor = new RecoveryMonitor();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates the worker.
     *
     * @param configuration the configuration
     * @param contentStoreComponent         the content store copied from
     * @param recoveryContentStoreComponent the content store copied to
     */
    public RecoveryWorker(DynamicConfiguration configuration,
                          ContentStoreComponent contentStoreComponent,
                          RecoveryContentStoreComponent recoveryContentStoreComponent) {
        this.configuration = configuration;
        this.contentStore = contentStoreComponent.getDelegate();
        this.recoveryContentStore = recoveryContentStoreComponent.getDelegate();
    }

    /**
     * Copies a topic.
     *
     * @param sourceTopic the topic copied from
     * @param targetTopic the topic copied to
     */
    public void recover(String sourceTopic, String targetTopic) {
        RawdataTopics.requireValid(sourceTopic);
        RawdataTopics.requireValid(targetTopic);
        LOG.info("Copy from {} to {}", sourceTopic, targetTopic);
        monitor.setStarted();
        Path dbLocation = getSequenceDatabaseLocation(configuration);
        monitor.setSourceDatabasePath(SequenceDbHelper.topicDirectory(dbLocation, sourceTopic));
        monitor.setSourceTopic(sourceTopic);
        monitor.setTargetTopic(targetTopic);
        try (LmdbEnvironment lmdbEnvironment = new LmdbEnvironment(configuration, dbLocation, sourceTopic)) {
            Dbi<ByteBuffer> sequenceDb = lmdbEnvironment.open();
            SequenceDbHelper sequenceDbHelper = new SequenceDbHelper(lmdbEnvironment, sequenceDb);
            PositionAndIdVersion firstPosition = sequenceDbHelper.findFirstPosition();
            monitor.setStartPosition(firstPosition.position());
            PositionAndIdVersion lastPosition = sequenceDbHelper.findLastPosition();
            monitor.setLastPosition(lastPosition.position());

            int publishAtCount = 1000;
            AtomicInteger bufferCounter = new AtomicInteger(0);
            List<String> bufferedPositions = new ArrayList<>(publishAtCount);
            ContentStream contentStream = contentStore.contentStream();
            ContentStream recoveryContentStream = recoveryContentStore.contentStream();
            AtomicLong lastTimestamp = new AtomicLong(0);
            try {
                ContentStreamProducer producer = recoveryContentStream.producer(targetTopic);
                ContentStreamConsumer consumer = contentStream.consumer(sourceTopic);
                ContentStreamBuffer buffer;
                ContentStreamBuffer peekBuffer = null;
                while (!closed.get() && (buffer = consumer.receive(15, TimeUnit.SECONDS)) != null) {
                    peekBuffer = buffer;

                    monitor.setCurrentPosition(buffer.position());
                    producer.copy(buffer);
                    bufferedPositions.add(buffer.position());
                    monitor.incrementBufferedPositions();

                    if (bufferCounter.incrementAndGet() == publishAtCount) {
                        publishBuffers(bufferCounter, bufferedPositions, producer);
                    }

                    if (lastPosition.id().equals(buffer.id()) && lastPosition.position().equals(buffer.position())) {
                        if (!bufferedPositions.isEmpty()) {
                            publishBuffers(bufferCounter, bufferedPositions, producer);
                        }
                        break;
                    }
                }
                monitor.setEnded();
                if (peekBuffer != null) {
                    lastTimestamp.set(TimeOrderedIds.timestamp(peekBuffer.id()));
                }
                LOG.info("Successful recovery from {} to {}. Recovered {} positions.", sourceTopic, targetTopic, monitor.copiedPositions.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                monitor.setEnded();
                throw new RuntimeException(e);
            } catch (Exception e) {
                monitor.setEnded();
                throw new RuntimeException(e);
            } finally {
                // the producer writes the target topic and the consumer reads the source topic
                recoveryContentStream.closeAndRemoveProducer(targetTopic);
                contentStream.closeAndRemoveConsumer(sourceTopic);
            }

            if (closed.get()) {
                return;
            }

            // test tail from last recovered position
            if (lastTimestamp.get() == 0) {
                return;
            }

            postCheckTail(contentStream, targetTopic, lastTimestamp.get());
        }
    }

    // reads the target topic from the last recovered position and writes what follows it to a tail report
    private void postCheckTail(ContentStream contentStream, String targetTopic, long lastTimestamp) {
        ContentStreamConsumer consumer = contentStream.consumer(targetTopic);
        try {
            ContentStreamBuffer lastRecoveredMessage = contentStream.lastMessage(targetTopic);
            long fromTimestamp = lastRecoveredMessage != null && TimeOrderedIds.timestamp(lastRecoveredMessage.id()) < lastTimestamp ?
                    TimeOrderedIds.timestamp(lastRecoveredMessage.id()) :
                    lastTimestamp;
            if (LOG.isTraceEnabled()) {
                LOG.trace("Post check recovered positions from timestamp: {}", Instant.ofEpochMilli(fromTimestamp));
            }
            monitor.setPostCheckFromTimestamp(fromTimestamp);
            consumer.seek(fromTimestamp);
            ContentStreamBuffer buffer;
            Path reportPath = SequenceDbHelper.topicDirectory(getSequenceDatabaseLocation(configuration), targetTopic).resolve("report");
            Files.createDirectories(reportPath);
            try (JsonArrayWriter writer = new JsonArrayWriter(reportPath, "tail-report.json", 5000)) {
                while (!closed.get() && (buffer = consumer.receive(3, TimeUnit.SECONDS)) != null) {
                    if (monitor.postCheckStartPosition.get() == null) {
                        monitor.setPostCheckStartPosition(buffer.position());
                    }
                    monitor.setPostCheckLastPosition(buffer.position());
                    monitor.incrementPostCheckCheckedPositions();
                    ObjectNode positionNode = writer.parser().createObjectNode();
                    positionNode.put(buffer.id().toString(), buffer.position());
                    writer.write(positionNode);
                }
            }
            LOG.trace("Done post checking tail positions! Tail size is: {}", monitor.postCheckCheckedPositions.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            contentStream.closeAndRemoveConsumer(targetTopic);
        }
    }

    private void publishBuffers(AtomicInteger bufferCounter, List<String> bufferedPositions, ContentStreamProducer producer) {
        String[] publishPositions = bufferedPositions.toArray(new String[0]);
        producer.publish(publishPositions);
        monitor.incrementCopiedPositions(publishPositions.length);
        bufferCounter.set(0);
        bufferedPositions.clear();
        monitor.resetBufferedPositions();
    }

    /**
     * The recovery's progress.
     *
     * @return the monitor
     */
    public RecoveryMonitor monitor() {
        return monitor;
    }

    /**
     * The recovery's summary.
     *
     * @return the summary
     */
    public RecoveryMonitor.Summary summary() {
        return monitor.build();
    }

    /**
     * Stops the recovery.
     */
    public void terminate() {
        closed.set(true);
    }
}
