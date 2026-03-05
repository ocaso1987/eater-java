package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.ReadException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 字节源：基于 {@link ByteBuffer} 的按位置读取，不持有位置。
 */
public final class ByteSource extends ParseTarget<ByteBuffer> {

    public ByteSource(ByteBuffer buffer) {
        super(buffer == null ? null : buffer.slice());
    }

    /** 数据源长度（字节数）。 */
    public int getLength() {
        return getTarget().limit();
    }

    /** 校验位置在 [0, getLength()] 内，否则抛 {@link IllegalArgumentException}。 */
    public void validatePosition(int position) {
        if (position < 0 || position > getLength()) {
            throw new IllegalArgumentException("position out of range: " + position + ", length: " + getLength());
        }
    }

    /** 从给定位置起剩余字节数。 */
    public int remainingBytes(int position) {
        return getLength() - position;
    }

    /** 在指定位置读取一个字节。越界或不足时抛 {@link ReadException}。 */
    public byte readByte(int position) throws ReadException {
        requireReadablePosition(position);
        return getTarget().get(position);
    }

    /** 从指定位置读取 n 个字节。不足时抛 {@link ReadException}。 */
    public byte[] readBytes(int position, int n) throws ReadException {
        requireBytesAvailable(position, n);
        byte[] arr = new byte[n];
        getTarget().get(position, arr, 0, n);
        return arr;
    }

    private void requireReadablePosition(int position) throws ReadException {
        ByteBuffer buf = getTarget();
        if (position >= buf.limit()) {
            ReadException ex = new ReadException("no remaining bytes");
            ex.addContextValue("position", position);
            throw ex;
        }
        if (position < 0) {
            ReadException ex = new ReadException("index out of range: " + position + ", length: " + getTarget().limit());
            ex.addContextValue("index", position);
            ex.addContextValue("length", getTarget().limit());
            throw ex;
        }
    }

    private void requireBytesAvailable(int position, int n) throws ReadException {
        if (position < 0 || position + n > getTarget().limit()) {
            ReadException ex = new ReadException("insufficient bytes: need " + n + ", remaining " + remainingBytes(position));
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("remaining", remainingBytes(position));
            throw ex;
        }
    }

    public static ByteSource fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    public static ByteSource fromBytes(byte[] data, int offset, int length) {
        return new ByteSource(ByteBuffer.wrap(data, offset, length));
    }

    public static ByteSource fromByteBuffer(ByteBuffer buffer) {
        return new ByteSource(buffer);
    }

    public static ByteSource fromString(CharSequence text) {
        return fromString(text, StandardCharsets.UTF_8);
    }

    public static ByteSource fromString(CharSequence text, Charset charset) {
        return new ByteSource(charset.encode(CharBuffer.wrap(text.toString())));
    }
}
