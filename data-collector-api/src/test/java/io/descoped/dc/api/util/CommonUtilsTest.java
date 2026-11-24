package io.descoped.dc.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonUtilsTest {

    @Test
    void thatAFileIsReadAsUtf8(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("file.txt"), "blåbær", StandardCharsets.UTF_8);
        assertEquals("blåbær", CommonUtils.readFileAsUtf8(file.toString()));
    }

    @Test
    void thatAMissingFileIsAnUncheckedIOException(@TempDir Path dir) {
        String missing = dir.resolve("missing.txt").toString();
        assertThrows(UncheckedIOException.class, () -> CommonUtils.readFileAsUtf8(missing));
    }

    @Test
    void thatTheStackTraceIsPrinted() {
        assertDoesNotThrow(CommonUtils::printStackTrace);
    }
}
