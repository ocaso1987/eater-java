package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础测试：直接针对 {@link com.github.ocaso1987.eater.Parsers} 及上下文中的方法与行为。
 */
@SuppressWarnings("unchecked")
class ParsersTest {

    // ---------- bytes / oneByte / exactBytes / bytesUntil / bytesAsString ----------

    @Test
    void bytes_consumesN_returnsArrayAndAdvances() throws Exception {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3, 4, 5});
        byte[] result = bytes(3).parse(ctx);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(3, ctx.position());
        assertEquals(2, ctx.remainingBytes());
    }

    @Test
    void oneByte_consumesOne_returnsSingleByteArray() throws Exception {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{42});
        byte[] result = oneByte().parse(ctx);
        assertArrayEquals(new byte[]{42}, result);
        assertEquals(1, ctx.position());
    }

    @Test
    void exactBytes_match_consumesAndReturnsCopy() throws Exception {
        byte[] expected = "hel".getBytes(StandardCharsets.UTF_8);
        ParseContext ctx = ParseContext.fromBytes("hello".getBytes(StandardCharsets.UTF_8));
        byte[] result = exactBytes(expected).parse(ctx);
        assertArrayEquals(expected, result);
        assertEquals(3, ctx.position());
    }

    @Test
    void bytesUntil_delimiter_returnsSegmentWithoutConsumingDelimiter() throws Exception {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3, 0, 4, 5});
        byte[] result = bytesUntil((byte) 0).parse(ctx);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(3, ctx.position());
        assertEquals((byte) 0, ctx.readByte());
    }

    @Test
    void bytesAsUtf8_decodesToUtf8String() throws Exception {
        ParseContext ctx = ParseContext.fromBytes("Hi".getBytes(StandardCharsets.UTF_8));
        String result = bytesAsUtf8(2).parse(ctx);
        assertEquals("Hi", result);
        assertEquals(2, ctx.position());
    }

    @Test
    void bytes_insufficient_throwsReadExceptionAndKeepsPosition() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3});
        assertThrows(ReadException.class, () -> bytes(5).parse(ctx));
        assertEquals(0, ctx.position());
    }

    // ---------- chars / oneChar / exactString / charsUntil ----------

    @Test
    void chars_consumesN_returnsStringAndAdvances() throws Exception {
        ParseContext ctx = ParseContext.fromChars("hello");
        String result = chars(3).parse(ctx);
        assertEquals("hel", result);
        assertEquals(3, ctx.position());
    }

    @Test
    void oneChar_consumesOne_returnsChar() throws Exception {
        ParseContext ctx = ParseContext.fromChars("x");
        assertEquals('x', oneChar().parse(ctx));
        assertEquals(1, ctx.position());
    }

    @Test
    void exactString_match_consumesAndReturns() throws Exception {
        ParseContext ctx = ParseContext.fromChars("hello world");
        assertEquals("hello", exactString("hello").parse(ctx));
        assertEquals(5, ctx.position());
        assertEquals(" world", exactString(" world").parse(ctx));
    }

    @Test
    void charsUntil_delimiter_returnsSegmentWithoutConsumingDelimiter() throws Exception {
        ParseContext ctx = ParseContext.fromChars("a,b,c");
        assertEquals("a", charsUntil(',').parse(ctx));
        assertEquals(1, ctx.position());
        ctx.readChar();
        assertEquals("b", charsUntil(',').parse(ctx));
    }

    @Test
    void chars_insufficient_throwsReadExceptionAndKeepsPosition() {
        ParseContext ctx = ParseContext.fromChars("ab");
        assertThrows(ReadException.class, () -> chars(5).parse(ctx));
        assertEquals(0, ctx.position());
    }

    // ---------- optional ----------

    @Test
    void optional_success_returnsResultAndAdvances() throws Exception {
        ParseContext ctx = ParseContext.fromChars("ab");
        String result = optional(exactString("ab")).parse(ctx);
        assertEquals("ab", result);
        assertEquals(2, ctx.position());
    }

    @Test
    void optional_readException_rollsBackAndReturnsNull() throws Exception {
        ParseContext ctx = ParseContext.fromChars("ab");
        String result = optional(exactString("xy")).parse(ctx);
        assertNull(result);
        assertEquals(0, ctx.position());
    }

    // ---------- many (变参) ----------

    @Test
    void many_varargs_runsInOrderAndReturnsList() throws Exception {
        ParseContext ctx = ParseContext.fromChars("hello world");
        List<String> list = many(exactString("hello"), exactString(" world")).parse(ctx);
        assertEquals(List.of("hello", " world"), list);
        assertEquals(11, ctx.position());
    }

    // ---------- many (单解析器) ----------

    @Test
    void many_single_untilReadExceptionReturnsListAndStops() throws Exception {
        ParseContext ctx = ParseContext.fromChars("aaaX");
        Parser<List<String>> p = many(exactString("a"));
        List<String> list = p.parse(ctx);
        assertEquals(List.of("a", "a", "a"), list);
        assertEquals(3, ctx.position());
    }

    // ---------- choose ----------

    @Test
    void choose_firstSuccess_returnsResult() throws Exception {
        ParseContext ctx = ParseContext.fromChars("world");
        Parser<String> p = choose(exactString("hello"), exactString("world"));
        assertEquals("world", p.parse(ctx));
        assertEquals(5, ctx.position());
    }

    @Test
    void choose_empty_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> choose());
        assertThrows(IllegalArgumentException.class, () -> choose((Parser<String>[]) new Parser<?>[0]));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> choose()).getMessage().contains("no parsers provided"));
    }

    // ---------- repeat ----------

    @Test
    void repeat_nTimes_returnsList() throws Exception {
        ParseContext ctx = ParseContext.fromChars("ABC");
        List<String> list = repeat(map(oneChar(), c -> String.valueOf(c)), 3).parse(ctx);
        assertEquals(List.of("A", "B", "C"), list);
        assertEquals(3, ctx.position());
    }

    // ---------- map ----------

    @Test
    void map_appliesFunctionToResult() throws Exception {
        ParseContext ctx = ParseContext.fromChars("123");
        int n = map(chars(3), s -> Integer.parseInt(s)).parse(ctx);
        assertEquals(123, n);
        assertEquals(3, ctx.position());
    }

    // ---------- peek ----------

    @Test
    void peek_success_returnsResultWithoutConsuming() throws Exception {
        ParseContext ctx = ParseContext.fromChars("ab");
        String result = peek(exactString("ab")).parse(ctx);
        assertEquals("ab", result);
        assertEquals(0, ctx.position());
        assertEquals("ab", exactString("ab").parse(ctx));
    }

    @Test
    void peek_readException_returnsNullAndKeepsPosition() throws Exception {
        ParseContext ctx = ParseContext.fromChars("ab");
        assertNull(peek(exactString("xy")).parse(ctx));
        assertEquals(0, ctx.position());
    }

    // ---------- ParseContext 边界与模式 ----------

    @Test
    void restorePosition_outOfRange_throwsIllegalArgumentException() {
        ParseContext ctx = ParseContext.fromChars("abc");
        assertThrows(IllegalArgumentException.class, () -> ctx.restorePosition(10));
        assertThrows(IllegalArgumentException.class, () -> ctx.restorePosition(-1));
    }

    @Test
    void readByte_inCharMode_throwsUnsupportedOperationException() {
        ParseContext ctx = ParseContext.fromChars("a");
        assertThrows(UnsupportedOperationException.class, () -> ctx.readByte());
    }

    @Test
    void readChar_inByteMode_throwsUnsupportedOperationException() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{97});
        assertThrows(UnsupportedOperationException.class, () -> ctx.readChar());
    }

    // ---------- 字节流用例：魔数 + 长度 + 负载 ----------

    @Test
    void byteStream_magicLengthPayload_parsesCorrectly() throws Exception {
        byte[] magic = "DEMO".getBytes(StandardCharsets.UTF_8);
        String payload = "Hello, bytes!";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int len = payloadBytes.length;
        byte[] packet = new byte[4 + 4 + len];
        System.arraycopy(magic, 0, packet, 0, 4);
        packet[4] = (byte) (len & 0xFF);
        packet[5] = (byte) ((len >> 8) & 0xFF);
        packet[6] = (byte) ((len >> 16) & 0xFF);
        packet[7] = (byte) ((len >> 24) & 0xFF);
        System.arraycopy(payloadBytes, 0, packet, 8, len);

        ParseContext ctx = ParseContext.fromBytes(packet);
        byte[] parsedMagic = exactBytes(magic).parse(ctx);
        byte[] lenBytes = bytes(4).parse(ctx);
        int parsedLen = (lenBytes[0] & 0xFF) | ((lenBytes[1] & 0xFF) << 8)
            | ((lenBytes[2] & 0xFF) << 16) | ((lenBytes[3] & 0xFF) << 24);
        byte[] body = bytes(parsedLen).parse(ctx);

        assertEquals("DEMO", new String(parsedMagic, StandardCharsets.UTF_8));
        assertEquals(13, parsedLen);
        assertEquals("Hello, bytes!", new String(body, StandardCharsets.UTF_8));
    }
}
