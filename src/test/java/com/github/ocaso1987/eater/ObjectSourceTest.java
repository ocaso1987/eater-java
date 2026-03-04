package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ObjectSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ReadException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** ObjectSource OGNL getValue 的覆盖率测试。 */
class ObjectSourceTest {

    @Test
    void getValue_simpleProperty() throws ReadException {
        ParseContext ctx = ParseContext.fromObject(new Bean("alice", 30));
        ObjectSource src = (ObjectSource) ctx.getSource();
        assertEquals("alice", src.getValue("name"));
        assertEquals(30, src.getValue("age"));
    }

    @Test
    void getValue_withType() throws ReadException {
        ParseContext ctx = ParseContext.fromObject(new Bean("bob", 25));
        ObjectSource src = (ObjectSource) ctx.getSource();
        assertEquals("bob", src.getValue("name", String.class));
        assertEquals(25, src.getValue("age", Integer.class));
    }

    @Test
    void getValue_nullReturnsNull() throws ReadException {
        ParseContext ctx = ParseContext.fromObject(new Bean(null, 0));
        ObjectSource src = (ObjectSource) ctx.getSource();
        assertNull(src.getValue("name"));
        assertNull(src.getValue("name", String.class));
    }

    @Test
    void getValue_typeMismatch_throwsReadException() {
        ParseContext ctx = ParseContext.fromObject(new Bean("x", 10));
        ObjectSource src = (ObjectSource) ctx.getSource();
        ReadException ex = assertThrows(ReadException.class, () -> src.getValue("name", Integer.class));
        assertTrue(ex.getMessage().contains("type mismatch"));
    }

    @Test
    void getValue_invalidExpression_throwsReadException() {
        ParseContext ctx = ParseContext.fromObject(new Object());
        ObjectSource src = (ObjectSource) ctx.getSource();
        ReadException ex = assertThrows(ReadException.class, () -> src.getValue("@@invalid!!"));
        assertTrue(ex.getMessage().contains("OGNL"));
        assertNotNull(ex.getContextValues().get("expression"));
    }

    @Test
    void getValue_nestedProperty() throws ReadException {
        Bean child = new Bean("child", 1);
        Bean root = new Bean("root", 0);
        root.setChild(child);
        ParseContext ctx = ParseContext.fromObject(root);
        ObjectSource src = (ObjectSource) ctx.getSource();
        assertEquals("child", src.getValue("child.name"));
    }

    @Test
    void getValue_listIndex() throws ReadException {
        ParseContext ctx = ParseContext.fromObject(Map.of("items", List.of("a", "b")));
        ObjectSource src = (ObjectSource) ctx.getSource();
        assertEquals("a", src.getValue("items[0]"));
        assertEquals("b", src.getValue("items[1]"));
    }

    public static class Bean {
        private String name;
        private int age;
        private Bean child;

        public Bean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public Bean getChild() { return child; }
        public void setChild(Bean child) { this.child = child; }
    }
}
