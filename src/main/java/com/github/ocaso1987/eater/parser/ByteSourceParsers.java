package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ByteSource;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 字节源解析：固定长度、精确匹配、按分隔符等；转字符串见 {@link StringParsers}。
 */
public final class ByteSourceParsers {

    private ByteSourceParsers() {}

    /** 解析恰好 n 个字节，返回字节数组。 */
    public static Parser<byte[]> n(int n) {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            byte[] arr = s.readBytes(pos, n);
            ctx.setCurrentReadPosition(pos + n);
            return arr;
        };
    }

    /** 解析一个字节，返回长度为 1 的 byte 数组。 */
    public static Parser<byte[]> one() {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            byte b = s.readByte(pos);
            ctx.setCurrentReadPosition(pos + 1);
            return new byte[]{b};
        };
    }

    /** 必须匹配给定字节序列，否则抛 {@link ReadException}；匹配时消耗并返回该序列的副本。 */
    public static Parser<byte[]> expect(byte[] expected) {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int n = expected.length;
            int pos = ctx.currentReadPosition();
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
            ctx.setCurrentReadPosition(pos + n);
            return expected.clone();
        };
    }

    /** 解析到遇到分隔字节或末尾，返回中间字节（不包含分隔符）；遇分隔符即停且不消费。 */
    public static Parser<byte[]> until(byte delimiter) {
        return ctx -> {
            ByteSource s = (ByteSource) ctx.getSource();
            int pos = ctx.currentReadPosition();
            int count = 0;
            while (s.remainingBytes(pos + count) >= 1) {
                byte b = s.readByte(pos + count);
                if (b == delimiter) break;
                count++;
            }
            byte[] arr = s.readBytes(pos, count);
            ctx.setCurrentReadPosition(pos + count);
            return arr;
        };
    }
}
