package io.descoped.dc.core.handler;

import io.descoped.dc.api.http.BodyHandler;
import io.descoped.dc.api.util.CompressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class FileBodyHandler implements BodyHandler<Path> {

    private static final Logger LOG = LoggerFactory.getLogger(FileBodyHandler.class);

    private static final FileAttribute<Set<PosixFilePermission>> OWNER_ONLY = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private final Object lock = new Object();
    protected final Path file;
    private final AtomicReference<Flow.Subscription> subscription = new AtomicReference<>();

    public FileBodyHandler(Path file) {
        Objects.requireNonNull(file);
        this.file = file;
        if (!Files.isWritable(file)) {
            throw new IllegalStateException("The file '" + file.toString() + "' is NOT readable!");
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (!this.subscription.compareAndSet(null, subscription)) {
            subscription.cancel();
            return;
        }
        // We can handle whatever you've got
        subscription.request(Long.MAX_VALUE);
        LOG.info("Start download: {}", file);
    }

    public boolean hasRemaining(List<ByteBuffer> byteBuffers) {
        synchronized (lock) {
            for (ByteBuffer buf : byteBuffers) {
                if (buf.hasRemaining())
                    return true;
            }
        }
        return false;
    }

    private void appendToFile(List<ByteBuffer> byteBuffers) {
        synchronized (lock) {
            try {
                for (ByteBuffer buf : byteBuffers) {
                    byte[] bytes = new byte[buf.remaining()];
                    buf.get(bytes);
                    Files.write(file, bytes, StandardOpenOption.APPEND);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onNext(List<ByteBuffer> byteBuffers) {
        // incoming buffers are allocated by http client internally,
        // and won't be used anywhere except this place.
        // So it's free simply to store them for further processing.
        assert hasRemaining(byteBuffers);
        appendToFile(byteBuffers);
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(throwable);
    }

    @Override
    public void onComplete() {
        // try decompress received payload
        LOG.info("Download completed: {}", file);
        synchronized (lock) {
            tryDecompress();
        }
    }

    private void tryDecompress() {
        if (!CompressUtils.isGzipCompressed(file)) {
            return;
        }

        try {
            LOG.info("Decompress file: {}", file);
            Path tempFile = createOwnerOnlyTempFile();
            if (!Files.isWritable(tempFile)) {
                throw new IllegalStateException("Decompression temp file is not writable: " + tempFile.toString());
            }

            CompressUtils.gunzip(file, new FileOutputStream(tempFile.toFile()));

            LOG.info("Move file from: {} to {}", tempFile, file);
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static FileBodyHandler ofFile(Path file) {
        return new FileBodyHandler(file);
    }

    public Path getFile() {
        return file;
    }

    @Override
    public Path body() {
        return getFile();
    }

    /*
     * A temporary file only its owner can access: rw------- on a file system with POSIX permissions, and on one without
     * them (Windows) an ACL with a single entry, for the user running dc.
     */
    static Path createOwnerOnlyTempFile() throws IOException {
        String prefix = UUID.randomUUID().toString();
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return Files.createTempFile(prefix, null, OWNER_ONLY);
        }
        UserPrincipal owner = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        return Files.createTempFile(prefix, null, ownerOnlyAcl(owner));
    }

    static FileAttribute<List<AclEntry>> ownerOnlyAcl(UserPrincipal owner) {
        List<AclEntry> acl = List.of(AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .build());
        return new FileAttribute<>() {
            @Override
            public String name() {
                return "acl:acl";
            }

            @Override
            public List<AclEntry> value() {
                return acl;
            }
        };
    }
}
