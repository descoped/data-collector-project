package io.descoped.dc.server.content;

import io.descoped.config.DynamicConfiguration;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStream;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.util.CommonUtils;
import io.descoped.dc.application.spi.Service;
import io.descoped.rawdata.api.RawdataTopics;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exports a topic's messages to files, when {@code data.collector.rawdata.dump.enabled} is set.
 */
public class RawdataFileSystemService implements Service {

    private static final Logger LOG = LoggerFactory.getLogger(RawdataFileSystemService.class);

    private final DynamicConfiguration configuration;
    private final ContentStream contentStream;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private CompletableFuture<Void> consumerFuture;
    private Path workDir;

    /**
     * Creates the service.
     *
     * @param configuration the configuration
     * @param contentStoreComponent the content store
     */
    public RawdataFileSystemService(DynamicConfiguration configuration, ContentStoreComponent contentStoreComponent) {
        this.configuration = configuration;
        ContentStore contentStore = contentStoreComponent.getDelegate();
        this.contentStream = contentStore.contentStream();
    }

    @Override
    public boolean isEnabled() {
        return Boolean.parseBoolean(configuration.find("data.collector.rawdata.dump.enabled").orElse(null));
    }

    @Override
    public void start() {
        if (consumerFuture != null) {
            return;
        }

        if (!Boolean.parseBoolean(configuration.find("data.collector.rawdata.dump.enabled").orElse(null))) {
            return;
        }

        String location = configuration.find("data.collector.rawdata.dump.location").orElse(null);
        if (location == null) {
            LOG.warn("Unable to start file exporter. No location is defined");
            return;
        }

        Path targetPath;
        if (location.isEmpty()) {
            targetPath = Paths.get(".").toAbsolutePath().resolve("storage").normalize();
        } else {
            targetPath = Paths.get(location).toAbsolutePath().normalize();
        }

        String topic = configuration.find("data.collector.rawdata.dump.topic").orElse(null);
        if (!RawdataTopics.isValid(topic)) {
            LOG.warn("Unable to start file exporter. No valid topic is defined");
            return;
        }

        workDir = targetPath.resolve(topic);
        consumerFuture = createFuture(topic, workDir)
                .exceptionally(throwable -> {
                    LOG.error("Ended exceptionally with error: {}", CommonUtils.captureStackTrace(throwable));
                    if (throwable instanceof RuntimeException runtimeException) {
                        throw runtimeException;
                    } else if (throwable instanceof Error error) {
                        throw error;
                    } else {
                        throw new RuntimeException(throwable);
                    }
                });
    }

    @Override
    public void stop() {
        if (!closed.compareAndSet(false, true)) {
            int retryCount = 0;
            while (!consumerFuture.isDone()) {
                if (retryCount > 10) {
                    break;
                }
                nap(100);
                retryCount++;
            }
        }
    }

    @SuppressWarnings("try") // ContentStreamConsumer.close() is declared to throw Exception
    CompletableFuture<Void> createFuture(String topic, Path targetPath) {
        Metadata metadata = new Metadata();
        Detector detector = new DefaultDetector();

        return CompletableFuture.runAsync(() -> {
            LOG.info("Starting rawdata exporter!");
            ContentStreamBuffer buffer;
            try (ContentStreamConsumer consumer = contentStream.consumer(topic)) {
                while (!closed.get()) {
                    if ((buffer = consumer.receive(1, TimeUnit.SECONDS)) != null) {
                        writeBuffer(buffer, targetPath, detector, metadata);
                    }
                }
                LOG.info("Stop rawdata exporter!");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void writeBuffer(ContentStreamBuffer buffer, Path targetPath, Detector detector, Metadata metadata) throws IOException {
        // the position and the keys come from the source's data: each becomes one file name, never a path
        Path filePath = targetPath.resolve(fileName(buffer.position()));
        if (!Files.exists(filePath)) {
            Files.createDirectories(filePath);
        }

        for (String key : Objects.requireNonNull(buffer.keys(), "A buffer without keys")) {
            byte[] data = buffer.get(key);
            String content = new String(data, StandardCharsets.UTF_8);

            MediaType mediaType = detector.detect(TikaInputStream.get(data), metadata, new ParseContext());
            String subtype = fileSuffix(mediaType.getSubtype(), content, key);

            Path contentFilePath = filePath.resolve(fileName(key) + subtype);
            Files.write(contentFilePath, data);
        }
    }

    /**
     * A name as one file name: characters other than letters, digits and {@code . - * _} are percent-encoded, so a
     * separator or a drive letter cannot leave the directory, and {@code .} and {@code ..} are encoded too. Names of
     * letters, digits and {@code . - _} are kept as they are.
     */
    static String fileName(String name) {
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        return ".".equals(encoded) || "..".equals(encoded) ? encoded.replace(".", "%2E") : encoded;
    }

    private static String fileSuffix(String subtype, String content, String key) {
        if (!"plain".equals(subtype)) {
            return "." + subtype;
        }
        if (content.startsWith("{") || content.startsWith("[") && !key.endsWith(".json")) {
            return ".json";
        }
        if (content.startsWith("<")) {
            return ".xml";
        }
        return "";
    }

    /**
     * The directory the messages are exported to.
     *
     * @return the directory
     */
    public Path getWorkDir() {
        Objects.requireNonNull(workDir);
        return workDir;
    }

    void nap(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
