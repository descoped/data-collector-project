package io.descoped.dc.server.db;

import io.descoped.config.DynamicConfiguration;
import org.lmdbjava.Dbi;
import org.lmdbjava.Env;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.lmdbjava.DbiFlags.MDB_CREATE;

/**
 * The LMDB environment of a topic, in a directory of its own.
 */
public class LmdbEnvironment implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(LmdbEnvironment.class);

    private final Path databaseDir;
    private final Env<ByteBuffer> env;
    private final String topic;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final int mapSize;
    private Dbi<ByteBuffer> db;

    /**
     * Creates the environment's directory; the environment opens on first use.
     *
     * @param configuration the configuration
     * @param databaseDir   the parent directory
     * @param topic         the topic
     */
    public LmdbEnvironment(DynamicConfiguration configuration, Path databaseDir, String topic) {
        this.databaseDir = databaseDir.resolve(topic);
        createDirectories(this.databaseDir);
        this.topic = topic;
        mapSize = configuration != null ? configuration.find("data.collector.integrityCheck.dbSizeInMb").map(Integer::parseInt).orElse(50) : 50;
        env = createEnvironment();
    }

    /**
     * Deletes a directory and everything in it.
     *
     * @param path the directory
     */
    public static void removePath(Path path) {
        try {
            if (path.toFile().exists()) {
                try (Stream<Path> paths = Files.walk(path)) {
                    paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The environment.
     *
     * @return the environment
     */
    public Env<ByteBuffer> env() {
        return env;
    }

    /**
     * The largest key the environment takes.
     *
     * @return the size, in bytes
     */
    public int maxKeySize() {
        return env.getMaxKeySize();
    }

    /**
     * The environment's directory.
     *
     * @return the directory
     */
    public Path getDatabaseDir() {
        return databaseDir;
    }

    private void createDirectories(Path databaseDir) {
        if (!databaseDir.toFile().exists()) {
            try {
                Files.createDirectories(databaseDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Env<ByteBuffer> createEnvironment() {
        int numberOfDbs = 3;
        int dbSize = mapSize * 1024 * 1024;
        LOG.info("Create Lmdb database with numberOfDbs: {}, size: {}", numberOfDbs, dbSize);
        return Env.create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(dbSize)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(numberOfDbs)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(databaseDir.toFile());
    }

    /**
     * Opens the topic's database, creating it if needed.
     *
     * @return the database
     */
    public Dbi<ByteBuffer> open() {
        if (!closed.get() && db != null) {
            return db;
        }
        db = env.openDbi(topic, MDB_CREATE);
        return db;
    }

    void drop() {
        if (!closed.get() && db != null) {
            try (Txn<ByteBuffer> txn = env.txnWrite()) {
                db.drop(txn);
            }
        }
    }

    /**
     * Whether the environment is closed.
     *
     * @return true after it is closed
     */
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        // drop()
        if (closed.compareAndSet(false, true)) {
            env.close();
        }
    }
}
