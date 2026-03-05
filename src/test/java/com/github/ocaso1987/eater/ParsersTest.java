package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.CharSource;
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
        byte[] result = byte_n(3).parse(ctx);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(3, ctx.currentReadPosition());
        assertEquals(2, ((ByteSource) ctx.getSource()).remainingBytes(ctx.currentReadPosition()));
    }

    @Test
    void oneByte_consumesOne_returnsSingleByteArray() throws Exception {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{42});
        byte[] result = byte_one().parse(ctx);
        assertArrayEquals(new byte[]{42}, result);
        assertEquals(1, ctx.currentReadPosition());
    }

    @Test
    void exactBytes_match_consumesAndReturnsCopy() throws Exception {
        byte[] expected = "hel".getBytes(StandardCharsets.UTF_8);
        ParseContext ctx = ParseContext.fromBytes("hello".getBytes(StandardCharsets.UTF_8));
        byte[] result = byte_expect(expected).parse(ctx);
        assertArrayEquals(expected, result);
        assertEquals(3, ctx.currentReadPosition());
    }

    @Test
    void bytesUntil_delimiter_returnsSegmentWithoutConsumingDelimiter() throws Exception {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3, 0, 4, 5});
        byte[] result = byte_until((byte) 0).parse(ctx);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        assertEquals(3, ctx.currentReadPosition());
        ByteSource bs = (ByteSource) ctx.getSource();
        int pos = ctx.currentReadPosition();
        assertEquals((byte) 0, bs.readByte(pos));
        ctx.setCurrentReadPosition(pos + 1);
    }

    @Test
    void bytesAsUtf8_decodesToUtf8String() throws Exception {
        ParseContext ctx = ParseContext.fromBytes("Hi".getBytes(StandardCharsets.UTF_8));
        String result = byte_utf8(2).parse(ctx);
        assertEquals("Hi", result);
        assertEquals(2, ctx.currentReadPosition());
    }

    @Test
    void bytes_insufficient_throwsReadExceptionAndKeepsPosition() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1, 2, 3});
        assertThrows(ReadException.class, () -> byte_n(5).parse(ctx));
        assertEquals(0, ctx.currentReadPosition());
    }

    // ---------- chars / oneChar / exactString / charsUntil ----------

    @Test
    void chars_consumesN_returnsStringAndAdvances() throws Exception {
        ParseContext ctx = ParseContext.fromString("hello");
        String result = str_n(3).parse(ctx);
        assertEquals("hel", result);
        assertEquals(3, ctx.currentReadPosition());
    }

    @Test
    void oneChar_consumesOne_returnsChar() throws Exception {
        ParseContext ctx = ParseContext.fromString("x");
        assertEquals('x', char_one().parse(ctx));
        assertEquals(1, ctx.currentReadPosition());
    }

    @Test
    void exactString_match_consumesAndReturns() throws Exception {
        ParseContext ctx = ParseContext.fromString("hello world");
        assertEquals("hello", str_expect("hello").parse(ctx));
        assertEquals(5, ctx.currentReadPosition());
        assertEquals(" world", str_expect(" world").parse(ctx));
    }

    @Test
    void charsUntil_delimiter_returnsSegmentWithoutConsumingDelimiter() throws Exception {
        ParseContext ctx = ParseContext.fromString("a,b,c");
        assertEquals("a", str_until(',').parse(ctx));
        assertEquals(1, ctx.currentReadPosition());
        CharSource cs = (CharSource) ctx.getSource();
        int p = ctx.currentReadPosition();
        cs.readChar(p);
        ctx.setCurrentReadPosition(p + 1);
        assertEquals("b", str_until(',').parse(ctx));
    }

    @Test
    void chars_insufficient_throwsReadExceptionAndKeepsPosition() {
        ParseContext ctx = ParseContext.fromString("ab");
        assertThrows(ReadException.class, () -> str_n(5).parse(ctx));
        assertEquals(0, ctx.currentReadPosition());
    }

    // ---------- optional ----------

    @Test
    void optional_success_returnsResultAndAdvances() throws Exception {
        ParseContext ctx = ParseContext.fromString("ab");
        String result = optional(str_expect("ab")).parse(ctx);
        assertEquals("ab", result);
        assertEquals(2, ctx.currentReadPosition());
    }

    @Test
    void optional_readException_rollsBackAndReturnsNull() throws Exception {
        ParseContext ctx = ParseContext.fromString("ab");
        String result = optional(str_expect("xy")).parse(ctx);
        assertNull(result);
        assertEquals(0, ctx.currentReadPosition());
    }

    // ---------- many (变参) ----------

    @Test
    void many_varargs_runsInOrderAndReturnsList() throws Exception {
        ParseContext ctx = ParseContext.fromString("hello world");
        List<String> list = many(str_expect("hello"), str_expect(" world")).parse(ctx);
        assertEquals(List.of("hello", " world"), list);
        assertEquals(11, ctx.currentReadPosition());
    }

    // ---------- many (单解析器) ----------

    @Test
    void many_single_untilReadExceptionReturnsListAndStops() throws Exception {
        ParseContext ctx = ParseContext.fromString("aaaX");
        Parser<List<String>> p = many(str_expect("a"));
        List<String> list = p.parse(ctx);
        assertEquals(List.of("a", "a", "a"), list);
        assertEquals(3, ctx.currentReadPosition());
    }

    // ---------- choose ----------

    @Test
    void choose_firstSuccess_returnsResult() throws Exception {
        ParseContext ctx = ParseContext.fromString("world");
        Parser<String> p = choose(str_expect("hello"), str_expect("world"));
        assertEquals("world", p.parse(ctx));
        assertEquals(5, ctx.currentReadPosition());
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
        ParseContext ctx = ParseContext.fromString("ABC");
        List<String> list = repeat(map(char_one(), c -> String.valueOf(c)), 3).parse(ctx);
        assertEquals(List.of("A", "B", "C"), list);
        assertEquals(3, ctx.currentReadPosition());
    }

    // ---------- map ----------

    @Test
    void map_appliesFunctionToResult() throws Exception {
        ParseContext ctx = ParseContext.fromString("123");
        int n = map(str_n(3), s -> Integer.parseInt(s)).parse(ctx);
        assertEquals(123, n);
        assertEquals(3, ctx.currentReadPosition());
    }

    // ---------- peek ----------

    @Test
    void peek_success_returnsResultWithoutConsuming() throws Exception {
        ParseContext ctx = ParseContext.fromString("ab");
        String result = peek(str_expect("ab")).parse(ctx);
        assertEquals("ab", result);
        assertEquals(0, ctx.currentReadPosition());
        assertEquals("ab", str_expect("ab").parse(ctx));
    }

    @Test
    void peek_readException_returnsNullAndKeepsPosition() throws Exception {
        ParseContext ctx = ParseContext.fromString("ab");
        assertNull(peek(str_expect("xy")).parse(ctx));
        assertEquals(0, ctx.currentReadPosition());
    }

    // ---------- ParseContext 边界与模式 ----------

    @Test
    void setCurrentReadPosition_outOfRange_throwsIllegalArgumentException() {
        ParseContext ctx = ParseContext.fromString("abc");
        assertThrows(IllegalArgumentException.class, () -> ctx.setCurrentReadPosition(10));
        assertThrows(IllegalArgumentException.class, () -> ctx.setCurrentReadPosition(-1));
    }

    @Test
    void byteSource_requiredForByteRead_throwsClassCastWhenCharSource() {
        ParseContext ctx = ParseContext.fromString("a");
        assertThrows(ClassCastException.class, () -> { ByteSource s = (ByteSource) ctx.getSource(); });
    }

    @Test
    void charSource_requiredForCharRead_throwsClassCastWhenByteSource() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{97});
        assertThrows(ClassCastException.class, () -> { CharSource s = (CharSource) ctx.getSource(); });
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
        byte[] parsedMagic = byte_expect(magic).parse(ctx);
        byte[] lenBytes = byte_n(4).parse(ctx);
        int parsedLen = (lenBytes[0] & 0xFF) | ((lenBytes[1] & 0xFF) << 8)
            | ((lenBytes[2] & 0xFF) << 16) | ((lenBytes[3] & 0xFF) << 24);
        byte[] body = byte_n(parsedLen).parse(ctx);

        assertEquals("DEMO", new String(parsedMagic, StandardCharsets.UTF_8));
        assertEquals(13, parsedLen);
        assertEquals("Hello, bytes!", new String(body, StandardCharsets.UTF_8));
    }

    // ---------- 值源 val() ----------

    @Test
    void val_expression_returnsValueFromObject() throws Exception {
        ParseContext ctx = ParseContext.fromObject(new ValueTargetTest.Bean("alice", 30));
        assertEquals("alice", val("name").parse(ctx));
        assertEquals(30, val("age").parse(ctx));
    }

    @Test
    void val_expressionWithType_returnsTypedValue() throws Exception {
        ParseContext ctx = ParseContext.fromObject(new ValueTargetTest.Bean("bob", 25));
        assertEquals("bob", val("name", String.class).parse(ctx));
        assertEquals(25, val("age", Integer.class).parse(ctx));
    }
}
