package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 字符/字符串解析：固定长度、精确匹配、按分隔符等。
 */
public final class CharParsers {

    private CharParsers() {}

    /** 解析恰好 n 个字符，返回字符串。 */
    public static Parser<String> chars(int n) {
        return ctx -> new String(ctx.readChars(n));
    }

    /** 解析一个字符。 */
    public static Parser<Character> oneChar() {
        return ctx -> ctx.readChar();
    }

    /** 必须匹配给定字符串，否则抛 {@link ReadException}；匹配时消耗并返回该字符串。 */
    public static Parser<String> exactString(String expected) {
        return ctx -> {
            int n = expected.length();
            if (!ctx.hasChars(n)) {
                ReadException ex = new ReadException("insufficient chars for string \"" + expected + "\"");
                ex.addContextValue("position", ctx.position());
                ex.addContextValue("expected", expected);
                throw ex;
            }
            int pos = ctx.position();
            for (int i = 0; i < n; i++) {
                char c = ctx.readChar();
                if (c != expected.charAt(i)) {
                    ctx.restorePosition(pos);
                    ReadException ex = new ReadException("char mismatch at index " + i + ": expected '" + expected.charAt(i) + "', got '" + c + "'");
                    ex.addContextValue("position", pos + i);
                    ex.addContextValue("index", i);
                    throw ex;
                }
            }
            return expected;
        };
    }

    /** 解析到遇到分隔字符或末尾，返回中间字符串（不包含分隔符）；不消费分隔符。两遍扫描避免 StringBuilder 扩容。 */
    public static Parser<String> charsUntil(char delimiter) {
        return ctx -> {
            int pos = ctx.position();
            int count = 0;
            while (ctx.hasChars(1)) {
                char c = ctx.readChar();
                if (c == delimiter) {
                    ctx.restorePosition(pos + count);
                    break;
                }
                count++;
            }
            ctx.restorePosition(pos);
            return new String(ctx.readChars(count));
        };
    }
}
