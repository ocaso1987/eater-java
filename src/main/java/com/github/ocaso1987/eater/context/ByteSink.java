package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.WriteException;

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;

/**
 * 字节输出目标：基于 {@link ByteBuffer} 持有输出流信息，支持按随机位置写入，不持有位置。
 * 固定容量时越界抛 {@link WriteException}；{@link #growable()} 创建的可扩容实例会在空间不足时扩容，跳着写时用 0 填位占位。
 */
public final class ByteSink extends ParseTarget<ByteBuffer> {

    /** 当前目标视图；可扩容时随扩容/填位更新。 */
    private ByteBuffer buffer;
    private final boolean growable;
    private byte[] growableArray;
    private int growableLength;

    public ByteSink(ByteBuffer buffer) {
        super(buffer == null ? null : buffer.duplicate());
        this.buffer = buffer == null ? null : (ByteBuffer) super.getTarget();
        this.growable = false;
    }

    /** 仅用于 {@link #growable()}。 */
    private ByteSink() {
        super(ByteBuffer.allocate(0));
        this.growableArray = new byte[16];
        this.growableLength = 0;
        this.buffer = ByteBuffer.wrap(this.growableArray, 0, 0);
        this.growable = true;
    }

    @Override
    public ByteBuffer getTarget() {
        return buffer;
    }

    /** 创建可扩容的字节输出目标；空间不足时自动扩容，跳着写时中间空缺用 0 填位。 */
    public static ByteSink growable() {
        return new ByteSink();
    }

    /** 输出目标容量（字节数）。 */
    public int getCapacity() {
        return getTarget().capacity();
    }

    /** 当前可写长度（limit，字节数）。 */
    public int getLimit() {
        return getTarget().limit();
    }

    /** 校验位置在 [0, getLimit()] 内，否则抛 {@link IllegalArgumentException}。 */
    public void validatePosition(int position) {
        if (position < 0 || position > getLimit()) {
            throw new IllegalArgumentException("position out of range: " + position + ", limit: " + getLimit());
        }
    }

    /** 在指定位置写入一个字节。固定容量时越界或缓冲区只读抛 {@link WriteException}；可扩容时自动扩容并用 0 填位。 */
    public void writeByte(int position, byte b) throws WriteException {
        if (growable) {
            ensureCapacityAndFillGap(position, 1);
            growableArray[position] = b;
            growableLength = Math.max(growableLength, position + 1);
            updateGrowableBuffer();
        } else {
            requireWritableRange(position, 1);
            try {
                getTarget().put(position, b);
            } catch (ReadOnlyBufferException e) {
                WriteException ex = new WriteException("byte buffer is read-only", e);
                ex.addContextValue("position", position);
                throw ex;
            }
        }
    }

    /** 从指定位置写入 n 个字节。固定容量时空间不足或只读抛 {@link WriteException}；可扩容时自动扩容并用 0 填位。 */
    public void writeBytes(int position, byte[] src) throws WriteException {
        writeBytes(position, src, 0, src == null ? 0 : src.length);
    }

    /** 从指定位置写入 src[offset..offset+length)。固定容量时空间不足或只读抛 {@link WriteException}；可扩容时自动扩容并用 0 填位。 */
    public void writeBytes(int position, byte[] src, int offset, int length) throws WriteException {
        if (src == null || offset < 0 || length < 0 || offset + length > src.length) {
            WriteException ex = new WriteException("invalid write: src length " + (src == null ? 0 : src.length) + ", offset " + offset + ", length " + length);
            ex.addContextValue("position", position);
            throw ex;
        }
        if (growable) {
            ensureCapacityAndFillGap(position, length);
            System.arraycopy(src, offset, growableArray, position, length);
            growableLength = Math.max(growableLength, position + length);
            updateGrowableBuffer();
        } else {
            requireWritableRange(position, length);
            try {
                getTarget().put(position, src, offset, length);
            } catch (ReadOnlyBufferException e) {
                WriteException ex = new WriteException("byte buffer is read-only", e);
                ex.addContextValue("position", position);
                throw ex;
            }
        }
    }

    private void ensureCapacityAndFillGap(int position, int n) {
        int need = position + n;
        if (need <= growableArray.length) {
            if (position > growableLength) {
                Arrays.fill(growableArray, growableLength, position, (byte) 0);
            }
            return;
        }
        int newCap = Math.max(need, growableArray.length * 2);
        byte[] newArray = new byte[newCap];
        System.arraycopy(growableArray, 0, newArray, 0, growableLength);
        if (position > growableLength) {
            Arrays.fill(newArray, growableLength, position, (byte) 0);
        }
        growableArray = newArray;
    }

    private void updateGrowableBuffer() {
        buffer = ByteBuffer.wrap(growableArray, 0, growableLength);
    }

    private void requireWritableRange(int position, int n) throws WriteException {
        int limit = getTarget().limit();
        if (position < 0 || position + n > limit) {
            WriteException ex = new WriteException("write out of range: position " + position + ", length " + n + ", limit " + limit);
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("limit", limit);
            throw ex;
        }
    }

    public static ByteSink fromByteBuffer(ByteBuffer buffer) {
        return new ByteSink(buffer);
    }

    public static ByteSink fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    public static ByteSink fromBytes(byte[] data, int offset, int length) {
        return new ByteSink(ByteBuffer.wrap(data, offset, length));
    }
}
