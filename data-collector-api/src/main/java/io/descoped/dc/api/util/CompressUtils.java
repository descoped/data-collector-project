package io.descoped.dc.api.util;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip compression and decompression.
 */
public class CompressUtils {

    private CompressUtils() {
    }

    private static final int BUFFER_SIZE = 4 * 1024;

    /**
     * Whether a file starts with the gzip magic number.
     *
     * @param file the file
     * @return {@code true} if it is gzip compressed
     * @throws RuntimeException if the file cannot be read
     */
    public static boolean isGzipCompressed(Path file) {
        try {
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                byte[] buf = new byte[4];
                if (fis.read(buf) != -1) {
                    return isGzipCompressed(buf);
                }
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Whether bytes start with the gzip magic number.
     *
     * @param compressed the bytes, at least two
     * @return {@code true} if they are gzip compressed
     */
    public static boolean isGzipCompressed(final byte[] compressed) {
        return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    /**
     * Compresses bytes into a stream, and closes the stream.
     *
     * @param data         the bytes
     * @param outputStream the stream
     * @param <R>          the stream's type
     * @return the stream
     * @throws RuntimeException if writing fails
     */
    public static <R extends OutputStream> R gzip(byte[] data, R outputStream) {
        return gzip(new ByteArrayInputStream(data), outputStream);
    }

    /**
     * Compresses a file into a stream, and closes the stream.
     *
     * @param file         the file
     * @param outputStream the stream
     * @param <R>          the stream's type
     * @return the stream
     * @throws RuntimeException if the file is not found or writing fails
     */
    public static <R extends OutputStream> R gzip(Path file, R outputStream) {
        try {
            return gzip(new FileInputStream(file.toFile()), outputStream);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compresses a stream into another, and closes both.
     *
     * @param inputStream  the input
     * @param outputStream the output
     * @param <R>          the output's type
     * @return the output
     * @throws RuntimeException if reading or writing fails
     */
    public static <R extends OutputStream> R gzip(InputStream inputStream, R outputStream) {
        try (inputStream) {
            try (outputStream) {
                try (GZIPOutputStream gos = new GZIPOutputStream(outputStream)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        gos.write(buffer, 0, len);
                    }
                }
                return outputStream;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decompresses bytes into a stream, and closes the stream.
     *
     * @param data         the compressed bytes
     * @param outputStream the stream
     * @param <R>          the stream's type
     * @return the stream
     * @throws RuntimeException if the data is not gzip or writing fails
     */
    public static <R extends OutputStream> R gunzip(byte[] data, R outputStream) {
        return gunzip(new ByteArrayInputStream(data), outputStream);
    }

    /**
     * Decompresses a file into a stream, and closes the stream.
     *
     * @param file         the compressed file
     * @param outputStream the stream
     * @param <R>          the stream's type
     * @return the stream
     * @throws RuntimeException if the file is not found, is not gzip, or writing fails
     */
    public static <R extends OutputStream> R gunzip(Path file, R outputStream) {
        try {
            return gunzip(new FileInputStream(file.toFile()), outputStream);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decompresses a stream into another, and closes both.
     *
     * @param inputStream  the compressed input
     * @param outputStream the output
     * @param <R>          the output's type
     * @return the output
     * @throws RuntimeException if the input is not gzip or reading or writing fails
     */
    public static <R extends OutputStream> R gunzip(InputStream inputStream, R outputStream) {
        try (inputStream) {
            try (GZIPInputStream gis = new GZIPInputStream(inputStream)) {
                try (outputStream) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = gis.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                }
                return outputStream;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
