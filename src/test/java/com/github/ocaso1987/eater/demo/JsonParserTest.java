package com.github.ocaso1987.eater.demo;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.WriteException;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.ocaso1987.eater.Parsers.*;
import static org.junit.jupiter.api.Assertions.*;

/** JSON 解析用例：简单对象、嵌套对象、数组、数字/布尔/null、转义引号字符串。 */
class JsonParserTest {

    /** 双引号字符串，支持 \\ 与 \" 转义。 */
    static Parser<String> quotedString() {
        return ctx -> {
            str_expect("\"").parse(ctx);
            CharSource s = (CharSource) ctx.getSource();
            StringBuilder sb = new StringBuilder();
            while (s.remainingChars(ctx.currentReadPosition()) >= 1) {
                int pos = ctx.currentReadPosition();
                char c = s.readChar(pos);
                ctx.setCurrentReadPosition(pos + 1);
                if (c == '"') break;
                if (c == '\\') {
                    if (s.remainingChars(ctx.currentReadPosition()) < 1) throw new ReadException("unexpected end after backslash");
                    int next = ctx.currentReadPosition();
                    char n = s.readChar(next);
                    ctx.setCurrentReadPosition(next + 1);
                    sb.append(switch (n) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> n;
                    });
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        };
    }

    static Parser<Void> skipJsonWs() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            while (s.remainingChars(ctx.currentReadPosition()) >= 1) {
                int pos = ctx.currentReadPosition();
                char c = s.readChar(pos);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
                ctx.setCurrentReadPosition(pos + 1);
            }
            return null;
        };
    }

    /** 解析 JSON 值（对象、数组、字符串、数字、true/false/null）；需在解析前跳过空白。 */
    @SuppressWarnings("unchecked")
    static Parser<Object> jsonValue() {
        return ctx -> {
            skipJsonWs().parse(ctx);
            CharSource s = (CharSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            char c = s.readChar(pos);
            if (c == '"') return quotedString().parse(ctx);
            if (c == '{') return jsonObject().parse(ctx);
            if (c == '[') return jsonArray().parse(ctx);
            if (c == 't') {
                str_expect("true").parse(ctx);
                return Boolean.TRUE;
            }
            if (c == 'f') {
                str_expect("false").parse(ctx);
                return Boolean.FALSE;
            }
            if (c == 'n') {
                str_expect("null").parse(ctx);
                return null;
            }
            if (c == '-' || (c >= '0' && c <= '9')) return jsonNumber().parse(ctx);
            throw new ReadException("unexpected char: " + c);
        };
    }

    static Parser<Number> jsonNumber() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            StringBuilder sb = new StringBuilder();
            int pos = ctx.currentReadPosition();
            char c = s.readChar(pos);
            if (c == '-') {
                sb.append(c);
                ctx.setCurrentReadPosition(pos + 1);
            }
            pos = ctx.currentReadPosition();
            while (s.remainingChars(pos) >= 1) {
                c = s.readChar(pos);
                if (c < '0' || c > '9') break;
                sb.append(c);
                ctx.setCurrentReadPosition(pos + 1);
                pos = ctx.currentReadPosition();
            }
            if (sb.length() == 0 || (sb.length() == 1 && sb.charAt(0) == '-'))
                throw new ReadException("invalid number");
            pos = ctx.currentReadPosition();
            if (s.remainingChars(pos) >= 1 && s.readChar(pos) == '.') {
                sb.append('.');
                ctx.setCurrentReadPosition(pos + 1);
                pos = ctx.currentReadPosition();
                while (s.remainingChars(pos) >= 1) {
                    c = s.readChar(pos);
                    if (c < '0' || c > '9') break;
                    sb.append(c);
                    ctx.setCurrentReadPosition(pos + 1);
                    pos = ctx.currentReadPosition();
                }
            }
            pos = ctx.currentReadPosition();
            if (s.remainingChars(pos) >= 1 && (s.readChar(pos) == 'e' || s.readChar(pos) == 'E')) {
                sb.append((char) s.readChar(pos));
                ctx.setCurrentReadPosition(pos + 1);
                pos = ctx.currentReadPosition();
                if (s.remainingChars(pos) >= 1 && (s.readChar(pos) == '+' || s.readChar(pos) == '-')) {
                    sb.append((char) s.readChar(pos));
                    ctx.setCurrentReadPosition(pos + 1);
                    pos = ctx.currentReadPosition();
                }
                while (s.remainingChars(pos) >= 1) {
                    c = s.readChar(pos);
                    if (c < '0' || c > '9') break;
                    sb.append(c);
                    ctx.setCurrentReadPosition(pos + 1);
                    pos = ctx.currentReadPosition();
                }
            }
            String num = sb.toString();
            if (num.contains(".") || num.toLowerCase().contains("e"))
                return Double.parseDouble(num);
            return Long.parseLong(num);
        };
    }

    static Parser<Map<String, Object>> jsonObject() {
        return ctx -> {
            str_expect("{").parse(ctx);
            Map<String, Object> obj = new LinkedHashMap<>();
            skipJsonWs().parse(ctx);
            CharSource s = (CharSource) ctx.getSource();
            boolean first = true;
            while (s.remainingChars(ctx.currentReadPosition()) >= 1) {
                int pos = ctx.currentReadPosition();
                if (s.readChar(pos) == '}') {
                    ctx.setCurrentReadPosition(pos + 1);
                    break;
                }
                if (!first) {
                    str_expect(",").parse(ctx);
                    skipJsonWs().parse(ctx);
                }
                first = false;
                String key = quotedString().parse(ctx);
                skipJsonWs().parse(ctx);
                str_expect(":").parse(ctx);
                Object value = jsonValue().parse(ctx);
                obj.put(key, value);
                skipJsonWs().parse(ctx);
            }
            return obj;
        };
    }

    static Parser<List<Object>> jsonArray() {
        return ctx -> {
            str_expect("[").parse(ctx);
            List<Object> list = new ArrayList<>();
            skipJsonWs().parse(ctx);
            CharSource s = (CharSource) ctx.getSource();
            boolean first = true;
            while (s.remainingChars(ctx.currentReadPosition()) >= 1) {
                int pos = ctx.currentReadPosition();
                if (s.readChar(pos) == ']') {
                    ctx.setCurrentReadPosition(pos + 1);
                    break;
                }
                if (!first) {
                    str_expect(",").parse(ctx);
                    skipJsonWs().parse(ctx);
                }
                first = false;
                list.add(jsonValue().parse(ctx));
                skipJsonWs().parse(ctx);
            }
            return list;
        };
    }

    @Test
    void json_simpleObjectKeyValues_parsesToMap() throws ReadException, WriteException, ParseException {
        String json = "{\"name\":\"Alice\",\"age\":\"30\",\"city\":\"Beijing\"}";
        ParseContext ctx = ParseContext.fromString(json);

        str_expect("{").parse(ctx);
        Map<String, String> obj = new LinkedHashMap<>();
        Parser<String> keyParser = quotedString();
        Parser<String> valueParser = quotedString();

        CharSource src = (CharSource) ctx.getSource();
        boolean first = true;
        while (src.remainingChars(ctx.currentReadPosition()) >= 1) {
            if (!first) {
                if (optional(str_expect(",")).parse(ctx) == null) break;
            }
            first = false;
            String key = keyParser.parse(ctx);
            str_expect(":").parse(ctx);
            String value = valueParser.parse(ctx);
            obj.put(key, value);
        }
        str_expect("}").parse(ctx);

        assertEquals(3, obj.size());
        assertEquals("Alice", obj.get("name"));
        assertEquals("30", obj.get("age"));
        assertEquals("Beijing", obj.get("city"));
    }

    @Test
    void json_nestingArrayEscapedQuotes_parsesToMap() throws ReadException, WriteException, ParseException {
        String json = """
            {
                "nesting": { "inner object": {} },
                "an array": [1.5, true, null, 1e-6],
                "string with escaped double quotes" : "\\"quick brown foxes\\""
            }
            """;

        ParseContext ctx = ParseContext.fromString(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) jsonValue().parse(ctx);

        assertEquals(3, root.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> nesting = (Map<String, Object>) root.get("nesting");
        assertNotNull(nesting);
        assertEquals(1, nesting.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) nesting.get("inner object");
        assertNotNull(inner);
        assertTrue(inner.isEmpty());

        @SuppressWarnings("unchecked")
        List<Object> arr = (List<Object>) root.get("an array");
        assertNotNull(arr);
        assertEquals(4, arr.size());
        assertEquals(1.5, ((Number) arr.get(0)).doubleValue(), 1e-9);
        assertEquals(Boolean.TRUE, arr.get(1));
        assertNull(arr.get(2));
        assertEquals(1e-6, ((Number) arr.get(3)).doubleValue(), 1e-15);

        assertEquals("\"quick brown foxes\"", root.get("string with escaped double quotes"));
    }
}
