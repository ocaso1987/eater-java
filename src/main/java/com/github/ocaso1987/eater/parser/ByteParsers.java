package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.ParseContext;
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
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentPosition();
            byte[] arr = s.readBytes(pos, n);
            ctx.setCurrentPosition(pos + n);
            return arr;
        };
    }

    /** 解析一个字节，返回长度为 1 的 byte 数组。 */
    public static Parser<byte[]> oneByte() {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentPosition();
            byte b = s.readByte(pos);
            ctx.setCurrentPosition(pos + 1);
            return new byte[]{b};
        };
    }

    /** 必须匹配给定字节序列，否则抛 {@link ReadException}；匹配时消耗并返回该序列的副本。 */
    public static Parser<byte[]> exactBytes(byte[] expected) {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int n = expected.length;
            int pos = ctx.currentPosition();
            if (s.remainingBytes(pos) < n) {
                ReadException ex = new ReadException("insufficient bytes for expected length " + n);
                ex.addContextValue("position", pos);
                ex.addContextValue("required", n);
                throw ex;
            }
            for (int i = 0; i < n; i++) {
                byte b = s.readByte(pos + i);
                if (b != expected[i]) {
                    ReadException ex = new ReadException("byte mismatch at index " + i + ": expected " + expected[i] + ", got " + b);
                    ex.addContextValue("position", pos + i);
                    ex.addContextValue("index", i);
                    throw ex;
                }
            }
            ctx.setCurrentPosition(pos + n);
            return expected.clone();
        };
    }

    /** 解析到遇到分隔字节或末尾，返回中间字节（不包含分隔符）；遇分隔符即停且不消费。 */
    public static Parser<byte[]> bytesUntil(byte delimiter) {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentPosition();
            int count = 0;
            while (s.remainingBytes(pos + count) >= 1) {
                byte b = s.readByte(pos + count);
                if (b == delimiter) break;
                count++;
            }
            byte[] arr = s.readBytes(pos, count);
            ctx.setCurrentPosition(pos + count);
            return arr;
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
