package io.descoped.dc.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helpers for files, class path resources and stack traces.
 */
public class CommonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CommonUtils.class);

    private CommonUtils() {
    }

    /**
     * The working directory.
     *
     * @return the absolute, normalized path
     */
    public static Path currentPath() {
        return Paths.get(".").toAbsolutePath().normalize();
    }

    /**
     * The real path of a file.
     *
     * @param path the path
     * @return the real path
     * @throws RuntimeException if the file does not exist or cannot be read
     */
    public static Path realPath(String path) {
        try {
            return Paths.get(path).toRealPath().normalize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a file as UTF-8 if it exists, or else a system class path resource of the same path.
     *
     * @param path the path
     * @return the content
     */
    public static String readFileOrClasspathResource(String path) {
        String utf8Str;
        if (Files.exists(Paths.get(path))) {
            utf8Str = readFileAsUtf8(path);
        } else {
            utf8Str = getResourceAsString(path, StandardCharsets.UTF_8);
        }
        return utf8Str;
    }

    /**
     * Reads a system class path resource.
     *
     * @param path    the resource path
     * @param charset the charset of the content
     * @return the content
     * @throws RuntimeException if the resource cannot be read or decoded
     */
    public static String getResourceAsString(String path, Charset charset) {
        try {
            URLConnection conn = ClassLoader.getSystemResource(path).openConnection();
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                CharBuffer cbuf = CharBuffer.allocate(bytes.length);
                CoderResult coderResult = charset.newDecoder().decode(ByteBuffer.wrap(bytes), cbuf, true);
                if (coderResult.isError()) {
                    coderResult.throwException();
                }
                return cbuf.flip().toString();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads a file as UTF-8.
     *
     * @param pathStr the path
     * @return the content
     * @throws java.io.UncheckedIOException if the file does not exist
     * @throws RuntimeException            if the file cannot be read or decoded
     */
    public static String readFileAsUtf8(String pathStr) {
        try {
            Path path = Paths.get(pathStr);
            if (Files.notExists(path)) {
                throw new UncheckedIOException("File not found: " + pathStr, new NoSuchFileException(pathStr));
            }
            byte[] bytes = Files.readAllBytes(path);
            CharBuffer cbuf = CharBuffer.allocate(bytes.length);
            CoderResult coderResult = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes), cbuf, true);
            if (coderResult.isError()) {
                coderResult.throwException();
            }
            return cbuf.flip().toString();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The stack trace of a throwable, as text.
     *
     * @param e the throwable
     * @return the stack trace
     */
    public static String captureStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Logs the calling thread's stack trace, at level info.
     */
    public static void printStackTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        int skip = 2;
        for (StackTraceElement ste : st) {
            if (skip > 0) {
                skip--;
                continue;
            }
            pw.println("    " + ste.toString());
        }
        LOG.info("StackTrace:\n{}", sw);
    }

}
