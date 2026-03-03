package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字节解析：固定长度、精确匹配、按分隔符、按编码转字符串等。
 */
public final class ByteParsers {

    private ByteParsers() {}

    /** 解析恰好 n 个字节，返回字节数组。 */
    public static Parser<byte[]> bytes(int n) {
        return ctx -> ctx.readBytes(n);
    }

    /** 解析一个字节，返回长度为 1 的 byte 数组。 */
    public static Parser<byte[]> oneByte() {
        return ctx -> new byte[]{ctx.readByte()};
    }

    /** 必须匹配给定字节序列，否则抛 {@link ReadException}；匹配时消耗并返回该序列的副本。 */
    public static Parser<byte[]> exactBytes(byte[] expected) {
        return ctx -> {
            int n = expected.length;
            if (!ctx.hasBytes(n)) {
                ReadException ex = new ReadException("insufficient bytes for expected length " + n);
                ex.addContextValue("position", ctx.position());
                ex.addContextValue("required", n);
                throw ex;
            }
            int pos = ctx.position();
            for (int i = 0; i < n; i++) {
                byte b = ctx.readByte();
                if (b != expected[i]) {
                    ctx.restorePosition(pos);
                    ReadException ex = new ReadException("byte mismatch at index " + i + ": expected " + expected[i] + ", got " + b);
                    ex.addContextValue("position", pos + i);
                    ex.addContextValue("index", i);
                    throw ex;
                }
            }
            return expected.clone();
        };
    }

    /** 解析到遇到分隔字节或末尾，返回中间字节（不包含分隔符）；遇分隔符即停且不消费。两遍扫描避免中间缓冲扩容。 */
    public static Parser<byte[]> bytesUntil(byte delimiter) {
        return ctx -> {
            int pos = ctx.position();
            int count = 0;
            while (ctx.hasBytes(1)) {
                byte b = ctx.readByte();
                if (b == delimiter) {
                    ctx.restorePosition(pos + count);
                    break;
                }
                count++;
            }
            ctx.restorePosition(pos);
            return ctx.readBytes(count);
        };
    }

    /** 解析 n 字节并按指定编码解码为字符串。 */
    public static Parser<String> bytesAsString(int n, Charset charset) {
        return ComboParsers.map(bytes(n), bytes -> new String(bytes, charset));
    }

    /** 解析 n 字节并按 UTF-8 解码为字符串。 */
    public static Parser<String> bytesAsUtf8(int n) {
        return bytesAsString(n, StandardCharsets.UTF_8);
    }
}
