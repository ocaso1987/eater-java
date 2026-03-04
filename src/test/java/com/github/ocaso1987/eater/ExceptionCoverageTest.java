package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** ParseException、ReadException、ContextAwareException 的覆盖率测试。 */
class ExceptionCoverageTest {

    @Test
    void parseException_messageOnly() {
        ParseException e = new ParseException("bad format");
        assertEquals("bad format", e.getMessage());
        assertTrue(e.getContextValues().isEmpty());
    }

    @Test
    void parseException_withCause() {
        Throwable cause = new RuntimeException("root");
        ParseException e = new ParseException("wrapped", cause);
        assertEquals("wrapped", e.getMessage());
        assertSame(cause, e.getCause());
    }

    @Test
    void parseException_addContextValue_returnsThis() {
        ParseException e = new ParseException("err");
        ParseException same = e.addContextValue("pos", 5).addContextValue("key", "v");
        assertSame(e, same);
        assertEquals(5, e.getContextValues().get("pos"));
        assertEquals("v", e.getContextValues().get("key"));
        assertTrue(e.getMessage().contains("context:"));
        assertTrue(e.getMessage().contains("pos=5"));
        Set<String> labels = e.getContextLabels();
        assertTrue(labels.contains("pos"));
        assertTrue(labels.contains("key"));
    }

    @Test
    void readException_addContextValue_getMessage() {
        ReadException e = new ReadException("insufficient data");
        e.addContextValue("position", 10).addContextValue("required", 3);
        assertTrue(e.getMessage().contains("insufficient data"));
        assertTrue(e.getMessage().contains("position=10"));
        assertEquals(10, e.getContextValues().get("position"));
        assertEquals(3, e.getContextValues().get("required"));
    }

    @Test
    void readException_withCause() {
        Throwable cause = new IllegalStateException("underlying");
        ReadException e = new ReadException("read failed", cause);
        assertSame(cause, e.getCause());
    }
}
