package io.descoped.dc.api.el;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ELCastTest {

    private final ELCast cast = new ELCast();

    @Test
    void thatValuesAreCast() {
        assertEquals(1.5f, cast.toFloat("1.5"));
        assertEquals(1.5d, cast.toDouble("1.5"));
        assertEquals(42, cast.toInteger("42"));
        assertEquals(42L, cast.toLong("42"));
    }

    @Test
    void thatNullIsNull() {
        assertNull(cast.toFloat(null));
        assertNull(cast.toDouble(null));
        assertNull(cast.toInteger(null));
        assertNull(cast.toLong(null));
    }

    @Test
    void thatANumberFormatExceptionIsRethrown() {
        assertThrows(NumberFormatException.class, () -> cast.toFloat("x"));
        assertThrows(NumberFormatException.class, () -> cast.toDouble("x"));
        assertThrows(NumberFormatException.class, () -> cast.toInteger("x"));
        assertThrows(NumberFormatException.class, () -> cast.toLong("x"));
    }
}
