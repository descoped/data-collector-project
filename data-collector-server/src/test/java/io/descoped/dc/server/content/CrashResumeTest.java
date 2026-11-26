package io.descoped.dc.server.content;

import io.descoped.dc.api.Specification;
import io.descoped.dc.api.content.ContentStore;
import io.descoped.dc.api.content.ContentStoreInitializer;
import io.descoped.dc.api.content.ContentStreamBuffer;
import io.descoped.dc.api.content.ContentStreamConsumer;
import io.descoped.dc.api.node.builder.SpecificationBuilder;
import io.descoped.dc.core.executor.Worker;
import io.descoped.dc.test.server.TestServer;
import io.descoped.dc.test.server.TestServerExtension;
import io.descoped.service.provider.api.ProviderConfigurator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.descoped.dc.api.Builders.addContent;
import static io.descoped.dc.api.Builders.context;
import static io.descoped.dc.api.Builders.execute;
import static io.descoped.dc.api.Builders.get;
import static io.descoped.dc.api.Builders.nextPage;
import static io.descoped.dc.api.Builders.paginate;
import static io.descoped.dc.api.Builders.parallel;
import static io.descoped.dc.api.Builders.publish;
import static io.descoped.dc.api.Builders.regex;
import static io.descoped.dc.api.Builders.sequence;
import static io.descoped.dc.api.Builders.whenVariableIsNull;
import static io.descoped.dc.api.Builders.xpath;
import static org.junit.jupiter.api.Assertions.assertEquals;

/*
 * A run on the filesystem provider whose process crashes after its publishes returned: nothing is uploaded yet (the
 * upload window is an hour), and the next process starts from a copy of the buffer folder, as after a restart. The
 * resumed run must write every entry and page once (rawdata-client 3.2: a returned publish survives the crash, and a
 * topic's last message holds it once a producer of the topic exists, which the worker opens before it reads what is
 * committed).
 */
@ExtendWith(TestServerExtension.class)
class CrashResumeTest {

    @Inject
    TestServer testServer;

    SpecificationBuilder specification(int stopAt) {
        return Specification.start("crash", "pages", "loop")
                .configure(context()
                        .topic("crash")
                        .header("accept", "application/xml")
                        .variable("nextPosition", "${cast.toLong(contentStream.lastOrInitialPosition(0)) + 1}"))
                .function(paginate("loop")
                        .variable("fromPosition", "${nextPosition}")
                        .addPageContent("fromPosition")
                        .iterate(execute("page"))
                        .prefetchThreshold(15)
                        .until(whenVariableIsNull("nextPosition")))
                .function(get("page")
                        .url(testServer.testURL("/api/events?position=${fromPosition}&pageSize=10&stopAt=" + stopAt))
                        .pipe(sequence(xpath("/feed/entry"))
                                .expected(xpath("/entry/id")))
                        .pipe(nextPage()
                                .output("nextPosition", regex(xpath("/feed/link[@rel=\"next\"]/@href"), "(?<=[?&]position=)[^&]*")))
                        .pipe(parallel(xpath("/feed/entry"))
                                .variable("position", xpath("/entry/id"))
                                .pipe(addContent("${position}", "entry"))
                                .pipe(publish("${position}")))
                        .returnVariables("nextPosition"));
    }

    static Map<String, String> configuration(Path store, Path temp) {
        return Map.of(
                "content.stream.connector", "rawdata",
                "rawdata.client.provider", "filesystem",
                "filesystem.storage-folder", store.toString(),
                "local-temp-folder", temp.toString(),
                "avro-file.max.seconds", "3600",
                "avro-file.max.bytes", "67108864",
                "avro-file.sync.interval", "200",
                "listing.min-interval-seconds", "0");
    }

    static void run(ContentStore contentStore, Map<String, String> configuration, SpecificationBuilder specification) {
        Worker.newBuilder()
                .specification(specification)
                .configuration(configuration)
                .contentStore(contentStore)
                .keepContentStoreOpenOnWorkerCompletion(true)
                .build()
                .run();
    }

    @Test
    void thatARunResumedAfterACrashWritesEveryPositionOnce() throws Exception {
        Path root = Path.of("target", "crash-resume-test", UUID.randomUUID().toString()).toAbsolutePath();
        Path store = Files.createDirectories(root.resolve("store"));
        Path temp = Files.createDirectories(root.resolve("temp"));
        Path tempAfterCrash = root.resolve("temp-after-crash");

        // the first process: its publishes return, then it crashes before anything is uploaded. The store is left open
        // on purpose: closing it would upload its windows, which a crash does not. Its folders stay under target/
        Map<String, String> before = configuration(store, temp);
        ContentStore crashed = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", before);
        run(crashed, before, specification(30));
        copy(temp, tempAfterCrash);

        // the next process, on what the crash left; the feed has grown meanwhile. Its worker opens the topics' producers
        // before it reads what is committed (Worker.openTopics): without that, the crashed run's entries are written twice
        Map<String, String> after = configuration(store, tempAfterCrash);
        try (ContentStore resumed = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", after)) {
            run(resumed, after, specification(50));
        }

        Map<String, String> reading = configuration(store, Files.createDirectories(root.resolve("temp-reader")));
        try (ContentStore reader = ProviderConfigurator.configure(ContentStoreInitializer.class, "rawdata", reading)) {
            assertEquals(LongStream.rangeClosed(1, 50).boxed().toList(), positions(reader, "crash"), "every entry once, in order");
            assertEquals(List.of(1L, 11L, 21L, 31L, 41L), positions(reader, "crash-pages"), "every page once, in order");
        }
    }

    static void copy(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path target = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    static List<Long> positions(ContentStore contentStore, String topic) throws InterruptedException {
        List<Long> positions = new ArrayList<>();
        ContentStreamConsumer consumer = contentStore.contentStream().consumer(topic);
        ContentStreamBuffer buffer;
        while ((buffer = consumer.receive(2, TimeUnit.SECONDS)) != null) {
            positions.add(Long.parseLong(buffer.position()));
        }
        return positions;
    }
}
