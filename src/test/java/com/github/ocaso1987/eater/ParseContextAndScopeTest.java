package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ObjectSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.context.ParseScope;
import com.github.ocaso1987.eater.exception.ReadException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** ParseContext 工厂、currentScope、enterScope/exitScope 及 ParseScope、ObjectSource 的覆盖率测试。 */
class ParseContextAndScopeTest {

    @Test
    void fromObject_initialScopeIsRootWithTarget() {
        Object root = new Object();
        ParseContext ctx = ParseContext.fromObject(root);
        ParseScope scope = ctx.getCurrentScope();
        assertNotNull(scope);
        assertTrue(scope.isRoot());
        assertNull(scope.getParent());
        assertSame(root, scope.getTarget());
    }

    @Test
    void enterScope_whenNull_createsRootScope() {
        ParseContext ctx = ParseContext.fromBytes(new byte[]{1});
        assertNull(ctx.getCurrentScope());
        ParseScope entered = ctx.enterScope("child");
        assertNotNull(entered);
        assertTrue(entered.isRoot());
        assertSame("child", entered.getTarget());
        assertSame(entered, ctx.getCurrentScope());
    }

    @Test
    void enterScope_whenHasScope_createsChildScope() {
        ParseContext ctx = ParseContext.fromObject("root");
        ParseScope root = ctx.getCurrentScope();
        ParseScope child = ctx.enterScope("child");
        assertFalse(child.isRoot());
        assertSame(root, child.getParent());
        assertSame("child", child.getTarget());
        assertSame(child, ctx.getCurrentScope());
        ctx.enterScope("grandchild");
        ParseScope grand = ctx.getCurrentScope();
        assertSame(child, grand.getParent());
        assertSame("grandchild", grand.getTarget());
    }

    @Test
    void exitScope_returnsToParent() {
        ParseContext ctx = ParseContext.fromObject("root");
        ctx.enterScope("a");
        ctx.enterScope("b");
        assertEquals("b", ctx.getCurrentScope().getTarget());
        ctx.exitScope();
        assertEquals("a", ctx.getCurrentScope().getTarget());
        ctx.exitScope();
        assertEquals("root", ctx.getCurrentScope().getTarget());
        ctx.exitScope();
        assertNull(ctx.getCurrentScope());
    }

    @Test
    void exitScope_whenNull_staysNull() {
        ParseContext ctx = ParseContext.fromChars("x");
        ctx.exitScope();
        assertNull(ctx.getCurrentScope());
    }

    @Test
    void setCurrentScope() {
        ParseContext ctx = ParseContext.fromChars("ab");
        ParseScope scope = new ParseScope(null, "custom");
        ctx.setCurrentScope(scope);
        assertSame(scope, ctx.getCurrentScope());
    }

    @Test
    void setCurrentPosition_onObjectSource_throwsUnsupportedOperationException() {
        ParseContext ctx = ParseContext.fromObject(new Object());
        assertThrows(UnsupportedOperationException.class, () -> ctx.setCurrentPosition(0));
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
