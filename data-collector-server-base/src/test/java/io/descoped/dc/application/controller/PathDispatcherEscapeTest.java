package io.descoped.dc.application.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PathDispatcherEscapeTest {

    @Test
    void thatControlCharactersCannotAddLogLines() {
        assertEquals("/tasks/a\\u000a12:00:00 INFO forged", PathDispatcher.escaped("/tasks/a\n12:00:00 INFO forged"));
        assertEquals("/tasks/a\\u000d\\u0009b", PathDispatcher.escaped("/tasks/a\r\tb"));
        assertEquals("/tasks/æøå", PathDispatcher.escaped("/tasks/æøå"));
        assertNull(PathDispatcher.escaped(null));
    }
}
