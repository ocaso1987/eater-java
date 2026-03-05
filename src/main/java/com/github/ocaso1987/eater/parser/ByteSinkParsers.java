package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ByteSink;
import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.WriteException;

/**
 * 字节汇写入：向 {@link ByteSink} 按当前写位置写入字节并推进写位置。
 */
public final class ByteSinkParsers {

    private ByteSinkParsers() {}

    /** 写入一个字节，推进写位置 1，返回该字节组成的数组。 */
    public static Parser<byte[]> one(byte b) {
        return ctx -> {
            ByteSink sink = (ByteSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeByte(pos, b);
            ctx.setCurrentWritePosition(pos + 1);
            return new byte[]{b};
        };
    }

    /** 写入恰好 data 的全部字节，推进写位置 data.length，返回 data 的副本。 */
    public static Parser<byte[]> n(byte[] data) {
        return ctx -> {
            if (data == null) {
                return new byte[0];
            }
            ByteSink sink = (ByteSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeBytes(pos, data);
            ctx.setCurrentWritePosition(pos + data.length);
            return data.clone();
        };
    }

    /** 从指定位置写入 src[offset..offset+length)，推进写位置 length，返回写入的字节副本。 */
    public static Parser<byte[]> write(byte[] src, int offset, int length) {
        return ctx -> {
            if (src == null || length == 0) {
                return new byte[0];
            }
            ByteSink sink = (ByteSink) ctx.getSink();
            int pos = ctx.currentWritePosition();
            sink.writeBytes(pos, src, offset, length);
            ctx.setCurrentWritePosition(pos + length);
            byte[] result = new byte[length];
            System.arraycopy(src, offset, result, 0, length);
            return result;
        };
    }

    /** 写入 data 的全部字节，推进写位置，返回 data 的副本。 */
    public static Parser<byte[]> write(byte[] data) {
        return data == null ? n(new byte[0]) : n(data);
    }
}
