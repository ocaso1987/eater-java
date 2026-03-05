package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.CharSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.context.ParseTarget;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 从 ByteSource 或 CharSource 读取为字符串的解析器；同一方法同时支持两种源类型。
 */
public final class StringParsers {

    private StringParsers() {}

    /** 解析 n 个单元为字符串：ByteSource 按指定编码解码 n 字节，CharSource 取 n 个字符。 */
    public static Parser<String> asStr(int n, Charset charset) {
        return ctx -> {
            ParseTarget<?> src = ctx.getSource();
            int pos = ctx.currentReadPosition();
            if (src instanceof ByteSource s) {
                byte[] arr = s.readBytes(pos, n);
                ctx.setCurrentReadPosition(pos + n);
                return new String(arr, charset);
            }
            if (src instanceof CharSource s) {
                char[] arr = s.readChars(pos, n);
                ctx.setCurrentReadPosition(pos + n);
                return new String(arr);
            }
            throw new UnsupportedOperationException("asStr requires ByteSource or CharSource, got " + src.getClass().getSimpleName());
        };
    }

    /** 解析 n 个单元为字符串：ByteSource 按 UTF-8 解码 n 字节，CharSource 取 n 个字符。 */
    public static Parser<String> asUtf8(int n) {
        return asStr(n, StandardCharsets.UTF_8);
    }
}
