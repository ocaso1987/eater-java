package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.CharSink;
import com.github.ocaso1987.eater.context.ParseContext;
/**
 * 字符汇写入：向 {@link CharSink} 按当前写位置写入字符并推进写位置。
 */
public final class CharSinkParsers {

    private CharSinkParsers() {}

    /** 写入一个字符，推进写位置 1，返回该字符。 */
    public static Parser<Character> one(char c) {
        return ctx -> {
            CharSink sink = (CharSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeChar(pos, c);
            ctx.setCurrentWritePosition(pos + 1);
            return c;
        };
    }

    /** 写入恰好 s 的全部字符，推进写位置 s.length()，返回 s。 */
    public static Parser<String> n(String s) {
        return ctx -> {
            if (s == null || s.isEmpty()) {
                return s == null ? "" : s;
            }
            CharSink sink = (CharSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeChars(pos, s.toCharArray());
            ctx.setCurrentWritePosition(pos + s.length());
            return s;
        };
    }

    /** 从 chars[offset..offset+length) 写入字符，推进写位置 length，返回写入的字符串。 */
    public static Parser<String> write(char[] chars, int offset, int length) {
        return ctx -> {
            if (chars == null || length == 0) {
                return "";
            }
            CharSink sink = (CharSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeChars(pos, chars, offset, length);
            ctx.setCurrentWritePosition(pos + length);
            return new String(chars, offset, length);
        };
    }

    /** 写入 data 的全部字符，推进写位置，返回 data。 */
    public static Parser<String> write(String data) {
        return data == null ? n("") : n(data);
    }
}
