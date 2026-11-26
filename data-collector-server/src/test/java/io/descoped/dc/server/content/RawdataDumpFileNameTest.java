package io.descoped.dc.server.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rawdata dump writes positions and keys, which come from the source's data, as single file names.
 */
class RawdataDumpFileNameTest {

    @Test
    void thatPlainNamesAreKept() {
        assertEquals("1234", RawdataFileSystemService.fileName("1234"));
        assertEquals("entry", RawdataFileSystemService.fileName("entry"));
        assertEquals("2020-01-01T00.00_a-b", RawdataFileSystemService.fileName("2020-01-01T00.00_a-b"));
    }

    @Test
    void thatSeparatorsAndDotNamesCannotLeaveTheDirectory() {
        assertEquals("..%2F..%2Fopt%2Fdc%2Fx", RawdataFileSystemService.fileName("../../opt/dc/x"));
        assertEquals("%2Fetc%2Fpasswd", RawdataFileSystemService.fileName("/etc/passwd"));
        assertEquals("C%3A%5Cx", RawdataFileSystemService.fileName("C:\\x"));
        assertEquals("%2E%2E", RawdataFileSystemService.fileName(".."));
        assertEquals("%2E", RawdataFileSystemService.fileName("."));
    }
}
