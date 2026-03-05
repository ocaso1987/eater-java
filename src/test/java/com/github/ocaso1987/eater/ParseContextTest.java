package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ValueTarget;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.context.ParseScope;
import com.github.ocaso1987.eater.exception.ReadException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** ParseContext 工厂、currentInputScope、enterInputScope/exitInputScope 及 ParseScope、ValueTarget 的覆盖率测试。 */
class ParseContextTest {

    @Test
    void fromObject_initialScopeIsRootWithTarget() {
        Object root = new Object();
        ParseContext ctx = ParseContext.fromObject(root);
        ParseScope scope = ctx.getCurrentInputScope();
        assertNotNull(scope);
        assertTrue(scope.isRoot());
        assertNull(scope.getParent());
        assertSame(root, scope.getTarget());
    }

    @Test
    void enterInputScope_whenNull_createsRootScope() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1});
        assertNull(ctx.getCurrentInputScope());
        ParseScope entered = ctx.enterInputScope("child");
        assertNotNull(entered);
        assertTrue(entered.isRoot());
        assertSame("child", entered.getTarget());
        assertSame(entered, ctx.getCurrentInputScope());
    }

    @Test
    void enterInputScope_whenHasScope_createsChildScope() {
        ParseContext ctx = ParseContext.fromObject("root");
        ParseScope root = ctx.getCurrentInputScope();
        ParseScope child = ctx.enterInputScope("child");
        assertFalse(child.isRoot());
        assertSame(root, child.getParent());
        assertSame("child", child.getTarget());
        assertSame(child, ctx.getCurrentInputScope());
        ctx.enterInputScope("grandchild");
        ParseScope grand = ctx.getCurrentInputScope();
        assertSame(child, grand.getParent());
        assertSame("grandchild", grand.getTarget());
    }

    @Test
    void exitInputScope_returnsToParent() {
        ParseContext ctx = ParseContext.fromObject("root");
        ctx.enterInputScope("a");
        ctx.enterInputScope("b");
        assertEquals("b", ctx.getCurrentInputScope().getTarget());
        ctx.exitInputScope();
        assertEquals("a", ctx.getCurrentInputScope().getTarget());
        ctx.exitInputScope();
        assertEquals("root", ctx.getCurrentInputScope().getTarget());
        ctx.exitInputScope();
        assertNull(ctx.getCurrentInputScope());
    }

    @Test
    void exitInputScope_whenNull_staysNull() {
        ParseContext ctx = ParseContext.fromString("x");
        ctx.exitInputScope();
        assertNull(ctx.getCurrentInputScope());
    }

    @Test
    void setCurrentInputScope() {
        ParseContext ctx = ParseContext.fromString("ab");
        ParseScope scope = new ParseScope(null, "custom");
        ctx.setCurrentInputScope(scope);
        assertSame(scope, ctx.getCurrentInputScope());
    }

    @Test
    void setCurrentReadPosition_onValueTarget_throwsUnsupportedOperationException() {
        ParseContext ctx = ParseContext.fromObject(new Object());
        assertThrows(UnsupportedOperationException.class, () -> ctx.setCurrentReadPosition(0));
    }

    @Test
    void fromObject_nullRoot_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ParseContext.fromObject(null));
    }

    @Test
    void fromBytes_offsetLength() throws ReadException {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        ParseContext ctx = ParseContext.fromBytes(data, 1, 3);
        ByteSource s = (ByteSource) ctx.getSource();
        assertEquals(3, s.getLength());
        assertEquals(2, s.readByte(0));
    }

    @Test
    void fromByteBuffer() {
        ParseContext ctx = ParseContext.fromByteBuffer(ByteBuffer.wrap(new byte[]{10, 20}));
        assertEquals(2, ((ByteSource) ctx.getSource()).getLength());
    }

    @Test
    void fromEncodedBytesUtf8() {
        ParseContext ctx = ParseContext.fromEncodedBytesUtf8("Hi");
        assertEquals(2, ((ByteSource) ctx.getSource()).getLength());
    }

    @Test
    void fromEncodedBytes_withCharset() {
        ParseContext ctx = ParseContext.fromEncodedBytes("AB", StandardCharsets.UTF_8);
        assertEquals(2, ((ByteSource) ctx.getSource()).getLength());
    }

    @Test
    void fromString_null_throwsNPE() {
        assertThrows(NullPointerException.class, () -> ParseContext.fromString(null));
    }

    // ---------- ParseScope ----------

    @Test
    void parseScope_enter_exit_isRoot() {
        ParseScope root = new ParseScope(null, "r");
        assertTrue(root.isRoot());
        assertNull(root.exit());
        ParseScope child = root.enter("c");
        assertFalse(child.isRoot());
        assertSame(root, child.getParent());
        assertSame("c", child.getTarget());
        assertSame(root, child.exit());
    }

    @Test
    void parseScope_enter_nullChild_throwsNPE() {
        ParseScope root = new ParseScope(null, "r");
        assertThrows(NullPointerException.class, () -> root.enter(null));
    }
}
