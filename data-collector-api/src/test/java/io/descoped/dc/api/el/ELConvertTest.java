package io.descoped.dc.api.el;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ELConvertTest {

    @Test
    void thatUtcDateToEpocReadsItsArgument() {
        ELConvert convert = new ELConvert();
        assertEquals(1601856000L, convert.utcDateToEpoc("2020-10-05T00:00:00.000Z"));
        assertEquals(1609459200L, convert.utcDateToEpoc("2021-01-01T00:00:00Z"));
        assertEquals(1609455600L, convert.utcDateToEpoc("2021-01-01T00:00:00+01:00"));
        assertEquals(1609459200L, convert.utcDateToEpoc("2021-01-01T00:00:00"), "without an offset, the date is read as UTC");
    }
}
