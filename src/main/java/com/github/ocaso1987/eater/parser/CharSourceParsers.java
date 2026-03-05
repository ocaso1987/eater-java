package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 字符源解析：固定长度、精确匹配、按分隔符等。
 */
public final class CharSourceParsers {

    private CharSourceParsers() {}

    /** 解析恰好 n 个字符，返回字符串。 */
    public static Parser<String> n(int n) {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            char[] arr = s.readChars(pos, n);
            ctx.setCurrentReadPosition(pos + n);
            return new String(arr);
        };
    }

    /** 解析一个字符。 */
    public static Parser<Character> one() {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            char c = s.readChar(pos);
            ctx.setCurrentReadPosition(pos + 1);
            return c;
        };
    }

    /** 必须匹配给定字符串，否则抛 {@link ReadException}；匹配时消耗并返回该字符串。 */
    public static Parser<String> expect(String expected) {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            int n = expected.length();
            int pos = ctx.currentReadPosition();
            if (s.remainingChars(pos) < n) {
                ReadException ex = new ReadException("insufficient chars for string \"" + expected + "\"");
                ex.addContextValue("position", pos);
                ex.addContextValue("expected", expected);
                throw ex;
            }
            for (int i = 0; i < n; i++) {
                char c = s.readChar(pos + i);
                if (c != expected.charAt(i)) {
                    ReadException ex = new ReadException("char mismatch at index " + i + ": expected '" + expected.charAt(i) + "', got '" + c + "'");
                    ex.addContextValue("position", pos + i);
                    ex.addContextValue("index", i);
                    throw ex;
                }
            }
            ctx.setCurrentReadPosition(pos + n);
            return expected;
        };
    }

    /** 解析到遇到分隔字符或末尾，返回中间字符串（不包含分隔符）；不消费分隔符。 */
    public static Parser<String> until(char delimiter) {
        return ctx -> {
            CharSource s = (CharSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            int count = 0;
            while (s.remainingChars(pos + count) >= 1) {
                char c = s.readChar(pos + count);
                if (c == delimiter) break;
                count++;
            }
            String result = new String(s.readChars(pos, count));
            ctx.setCurrentReadPosition(pos + count);
            return result;
        };
    }
}
