package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.exception.WriteException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** ByteSource、CharSource、Parsers 未覆盖分支的补充测试。 */
class SourceCoverageTest {

    @Test
    void byteSource_validatePosition_negative_throws() {
        ByteSource s = ByteSource.fromBytes(new byte[]{1, 2});
        assertThrows(IllegalArgumentException.class, () -> s.validatePosition(-1));
    }

    @Test
    void byteSource_validatePosition_overLength_throws() {
        ByteSource s = ByteSource.fromBytes(new byte[]{1, 2});
        assertThrows(IllegalArgumentException.class, () -> s.validatePosition(3));
    }

    @Test
    void byteSource_readByte_atLimit_throwsReadException() {
        ByteSource s = ByteSource.fromBytes(new byte[]{1});
        assertThrows(ReadException.class, () -> s.readByte(1));
    }

    @Test
    void byteSource_readByte_negativePosition_throwsReadException() {
        ByteSource s = ByteSource.fromBytes(new byte[]{1});
        assertThrows(ReadException.class, () -> s.readByte(-1));
    }

    @Test
    void byteSource_readBytes_insufficient_throwsReadException() {
        ByteSource s = ByteSource.fromBytes(new byte[]{1, 2});
        ReadException ex = assertThrows(ReadException.class, () -> s.readBytes(0, 5));
        assertTrue(ex.getMessage().contains("insufficient"));
    }

    @Test
    void byteSource_fromString_withCharset() {
        ByteSource s = ByteSource.fromString("Hi", StandardCharsets.UTF_8);
        assertEquals(2, s.getLength());
    }

    @Test
    void charSource_validatePosition_negative_throws() {
        CharSource s = CharSource.fromChars("ab");
        assertThrows(IllegalArgumentException.class, () -> s.validatePosition(-1));
    }

    @Test
    void charSource_validatePosition_overLength_throws() {
        CharSource s = CharSource.fromChars("ab");
        assertThrows(IllegalArgumentException.class, () -> s.validatePosition(3));
    }

    @Test
    void charSource_readChars_nonStringCharSequence() throws ReadException {
        CharSequence seq = new StringBuilder("xyz");
        CharSource s = new CharSource(seq);
        char[] arr = s.readChars(0, 3);
        assertEquals("xyz", new String(arr));
    }

    @Test
    void charSource_readChar_atLimit_throwsReadException() {
        CharSource s = CharSource.fromChars("a");
        assertThrows(ReadException.class, () -> s.readChar(1));
    }

    @Test
    void charSource_readChars_insufficient_throwsReadException() {
        CharSource s = CharSource.fromChars("ab");
        assertThrows(ReadException.class, () -> s.readChars(0, 5));
    }

    @Test
    void parsers_bytesAsString_withCharset() throws ReadException, WriteException, ParseException {
        ParseContext ctx = ParseContext.fromBytes("A".getBytes(StandardCharsets.ISO_8859_1));
        String v = byte_asString(1, StandardCharsets.ISO_8859_1).parse(ctx);
        assertEquals("A", v);
    }

    @Test
    void byteSourceParsers_expect_insufficientBytes_throwsReadException() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2});
        ReadException ex = assertThrows(ReadException.class, () -> byte_expect(new byte[]{1, 2, 3, 4}).parse(ctx));
        assertTrue(ex.getMessage().contains("insufficient"));
    }

    @Test
    void byteSourceParsers_expect_byteMismatch_throwsReadException() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3});
        ReadException ex = assertThrows(ReadException.class, () -> byte_expect(new byte[]{1, 9, 3}).parse(ctx));
        assertTrue(ex.getMessage().contains("mismatch"));
    }

    @Test
    void byteSourceParsers_until_emptySource_returnsEmpty() throws ReadException, WriteException, ParseException {
        ParseContext ctx = ParseContext.fromBytes(new byte[0]);
        byte[] result = byte_until((byte) ',').parse(ctx);
        assertArrayEquals(new byte[0], result);
    }

    @Test
    void parseContext_setCurrentReadPosition_onObjectSource_throwsUnsupportedOperationException() {
        ParseContext ctx = ParseContext.fromObject(new Object());
        assertThrows(UnsupportedOperationException.class, () -> ctx.setCurrentReadPosition(0));
    }
}
