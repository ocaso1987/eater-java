package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ByteSink;
import com.github.ocaso1987.eater.context.CharSink;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.WriteException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** ByteSink、CharSink、ByteSinkParsers、CharSinkParsers 及 WriteException 的覆盖率测试。 */
class SinkCoverageTest {

    // ---------- ByteSink ----------

    @Test
    void byteSink_fromBytes_writesAndAdvances() throws Exception {
        byte[] buf = new byte[8];
        ByteSink sink = ByteSink.fromBytes(buf);
        ParseContext ctx = ParseContext.withByteSink(sink);
        byte_sink_one((byte) 1).parse(ctx);
        byte_sink_n(new byte[]{2, 3}).parse(ctx);
        assertEquals(3, ctx.currentWritePosition());
        assertEquals(1, buf[0]);
        assertEquals(2, buf[1]);
        assertEquals(3, buf[2]);
    }

    @Test
    void byteSink_write_withOffsetAndLength() throws Exception {
        byte[] buf = new byte[4];
        ByteSink sink = ByteSink.fromBytes(buf);
        ParseContext ctx = ParseContext.withByteSink(sink);
        byte[] src = new byte[]{10, 20, 30, 40, 50};
        byte_sink_write(src, 1, 3).parse(ctx);
        assertEquals(3, ctx.currentWritePosition());
        assertEquals(20, buf[0]);
        assertEquals(30, buf[1]);
        assertEquals(40, buf[2]);
    }

    @Test
    void byteSink_n_null_returnsEmptyAndDoesNotAdvance() throws Exception {
        byte[] buf = new byte[2];
        ParseContext ctx = ParseContext.withByteSink(ByteSink.fromBytes(buf));
        byte[] result = byte_sink_n(null).parse(ctx);
        assertArrayEquals(new byte[0], result);
        assertEquals(0, ctx.currentWritePosition());
    }

    @Test
    void byteSink_growable_expandsAndFillsGap() throws Exception {
        ByteSink sink = ByteSink.growable();
        sink.writeByte(0, (byte) 1);
        sink.writeByte(5, (byte) 2);
        ByteBuffer target = sink.getTarget();
        assertEquals(6, target.limit());
        assertEquals(1, target.get(0));
        assertEquals(2, target.get(5));
        assertEquals(0, target.get(1));
    }

    @Test
    void byteSink_growable_resizeBeyondInitialCapacity() throws Exception {
        ByteSink sink = ByteSink.growable();
        byte[] chunk = new byte[20];
        for (int i = 0; i < 20; i++) chunk[i] = (byte) i;
        sink.writeBytes(0, chunk);
        assertEquals(20, sink.getTarget().limit());
        assertEquals(19, sink.getTarget().get(19));
    }

    @Test
    void byteSink_getCapacity() {
        ByteSink sink = ByteSink.fromBytes(new byte[5]);
        assertEquals(5, sink.getCapacity());
    }

    @Test
    void byteSink_validatePosition_valid_doesNotThrow() {
        ByteSink sink = ByteSink.fromBytes(new byte[3]);
        sink.validatePosition(0);
        sink.validatePosition(3);
    }

    @Test
    void byteSink_fromByteBuffer_fixedCapacity() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(4);
        ByteSink sink = ByteSink.fromByteBuffer(buf);
        sink.writeBytes(0, new byte[]{1, 2, 3, 4});
        assertEquals(1, buf.get(0));
        assertEquals(4, buf.get(3));
    }

    @Test
    void byteSink_validatePosition_negative_throws() {
        ByteSink sink = ByteSink.fromBytes(new byte[2]);
        assertThrows(IllegalArgumentException.class, () -> sink.validatePosition(-1));
    }

    @Test
    void byteSink_validatePosition_overLimit_throws() {
        ByteSink sink = ByteSink.fromBytes(new byte[2]);
        assertThrows(IllegalArgumentException.class, () -> sink.validatePosition(3));
    }

    @Test
    void byteSink_write_outOfRange_throwsWriteException() {
        ByteSink sink = ByteSink.fromBytes(new byte[2]);
        WriteException ex = assertThrows(WriteException.class, () -> sink.writeBytes(0, new byte[]{1, 2, 3}));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @Test
    void byteSink_writeBytes_invalidOffsetLength_throwsWriteException() {
        ByteSink sink = ByteSink.fromBytes(new byte[10]);
        assertThrows(WriteException.class, () -> sink.writeBytes(0, new byte[]{1, 2}, 0, 5));
        assertThrows(WriteException.class, () -> sink.writeBytes(0, null, 0, 1));
    }

    @Test
    void byteSink_readOnlyBuffer_throwsWriteException() {
        ByteBuffer ro = ByteBuffer.allocate(2).asReadOnlyBuffer();
        ByteSink sink = ByteSink.fromByteBuffer(ro);
        WriteException ex = assertThrows(WriteException.class, () -> sink.writeByte(0, (byte) 1));
        assertTrue(ex.getMessage().contains("read-only"));
    }

    // ---------- CharSink ----------

    @Test
    void charSink_fromStringBuilder_writesAndAdvances() throws Exception {
        StringBuilder sb = new StringBuilder();
        CharSink sink = CharSink.fromStringBuilder(sb);
        ParseContext ctx = ParseContext.withCharSink(sink);
        char_sink_one('a').parse(ctx);
        char_sink_n("bc").parse(ctx);
        assertEquals(3, ctx.currentWritePosition());
        assertEquals("abc", sb.toString());
    }

    @Test
    void charSink_write_withOffsetAndLength() throws Exception {
        StringBuilder sb = new StringBuilder();
        ParseContext ctx = ParseContext.withCharSink(CharSink.fromStringBuilder(sb));
        char_sink_write(new char[]{'x', 'y', 'z', 'w'}, 1, 2).parse(ctx);
        assertEquals(2, ctx.currentWritePosition());
        assertEquals("yz", sb.toString());
    }

    @Test
    void charSink_n_nullOrEmpty_returnsEmptyString() throws Exception {
        StringBuilder sb = new StringBuilder();
        ParseContext ctx = ParseContext.withCharSink(CharSink.fromStringBuilder(sb));
        assertEquals("", char_sink_n(null).parse(ctx));
        assertEquals("", char_sink_n("").parse(ctx));
        assertEquals(0, ctx.currentWritePosition());
    }

    @Test
    void charSink_nonStringBuilder_throwsWriteException() {
        CharSink sink = CharSink.fromAppendable(new StringBuilder());
        assertTrue(sink.getTarget() instanceof StringBuilder);
        CharSink bad = CharSink.fromAppendable(new java.io.StringWriter());
        WriteException ex = assertThrows(WriteException.class, () -> bad.writeChar(0, 'a'));
        assertTrue(ex.getMessage().contains("StringBuilder"));
    }

    @Test
    void charSink_writeChars_invalidArgs_throwsWriteException() {
        CharSink sink = CharSink.fromStringBuilder(new StringBuilder());
        assertThrows(WriteException.class, () -> sink.writeChars(0, new char[]{'a'}, 0, 5));
        assertThrows(WriteException.class, () -> sink.writeChars(0, null, 0, 1));
    }

    @Test
    void charSink_skipWrite_fillsGapWithPlaceholder() throws Exception {
        StringBuilder sb = new StringBuilder();
        CharSink sink = CharSink.fromStringBuilder(sb);
        sink.writeChar(0, 'a');
        sink.writeChar(3, 'b');
        assertEquals(4, sb.length());
        assertEquals('a', sb.charAt(0));
        assertEquals('b', sb.charAt(3));
    }

    // ---------- Parsers 门面（写） ----------

    @Test
    void byte_sink_write_overload() throws Exception {
        byte[] buf = new byte[3];
        ParseContext ctx = ParseContext.withByteSink(ByteSink.fromBytes(buf));
        byte_sink_write(new byte[]{7, 8, 9}).parse(ctx);
        assertEquals(7, buf[0]);
        assertEquals(9, buf[2]);
    }

    @Test
    void char_sink_write_overload() throws Exception {
        StringBuilder sb = new StringBuilder();
        ParseContext ctx = ParseContext.withCharSink(CharSink.fromStringBuilder(sb));
        char_sink_write("xyz").parse(ctx);
        assertEquals("xyz", sb.toString());
    }

    // ---------- ParseContext output scope ----------

    @Test
    void parseContext_outputScope_getSet() {
        ParseContext ctx = ParseContext.withCharSink(CharSink.fromStringBuilder(new StringBuilder()));
        assertNull(ctx.getCurrentOutputScope());
        ctx.setCurrentOutputScope(new com.github.ocaso1987.eater.context.ParseScope(null, new Object()));
        assertNotNull(ctx.getCurrentOutputScope());
    }
}
