package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 解析上下文：持有 {@link ParseSource} 与当前读取位置，对外提供顺序读与位置回退，供 {@link Parser} 使用。
 */
public final class ParseContext {

    private final ParseSource source;
    private int position;

    private ParseContext(ParseSource source) {
        this.source = source;
        this.position = 0;
    }

    /** 返回当前数据源。 */
    public ParseSource getSource() {
        return source;
    }

    /** 当前读取位置（从 0 开始）。 */
    public int position() {
        return position;
    }

    /**
     * 将位置重置为指定值（用于回退，如 optional 失败时）。
     * @throws IllegalArgumentException 若 position 不在 [0, getSource().getLength()] 内
     */
    public void restorePosition(int position) {
        source.validatePosition(position);
        this.position = position;
    }

    /** 剩余可读字节数（仅字节模式有效）。 */
    public int remainingBytes() {
        return source.remainingBytes(position);
    }

    /** 读取一个字节并推进位置。仅字节模式，不足时抛 {@link ReadException}。 */
    public byte readByte() throws ReadException {
        byte b = source.readByte(position);
        position++;
        return b;
    }

    /** 读取 n 个字节到新数组并推进位置。仅字节模式，不足时抛 {@link ReadException}。 */
    public byte[] readBytes(int n) throws ReadException {
        byte[] arr = source.readBytes(position, n);
        position += n;
        return arr;
    }

    /** 是否还有至少 n 个字节可读。 */
    public boolean hasBytes(int n) {
        return source.remainingBytes(position) >= n;
    }

    /** 剩余可读字符数（仅字符模式有效）。 */
    public int remainingChars() {
        return source.remainingChars(position);
    }

    /** 读取一个字符并推进位置。仅字符模式，不足时抛 {@link ReadException}。 */
    public char readChar() throws ReadException {
        char c = source.readChar(position);
        position++;
        return c;
    }

    /** 读取 n 个字符到新数组并推进位置。仅字符模式，不足时抛 {@link ReadException}。 */
    public char[] readChars(int n) throws ReadException {
        char[] arr = source.readChars(position, n);
        position += n;
        return arr;
    }

    /** 是否还有至少 n 个字符可读。 */
    public boolean hasChars(int n) {
        return source.remainingChars(position) >= n;
    }

    // ---------- 工厂：字节流 ----------

    /** 基于字节数组构造。 */
    public static ParseContext fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    /** 基于字节数组指定区间构造。 */
    public static ParseContext fromBytes(byte[] data, int offset, int length) {
        return new ParseContext(ParseSource.fromBytes(data, offset, length));
    }

    /** 基于 ByteBuffer 构造。 */
    public static ParseContext fromByteBuffer(ByteBuffer buffer) {
        return new ParseContext(ParseSource.fromByteBuffer(buffer));
    }

    /** 将字符串按 UTF-8 编码为字节后以字节模式解析。 */
    public static ParseContext fromString(CharSequence text) {
        return fromString(text, StandardCharsets.UTF_8);
    }

    /** 将字符串按指定编码为字节后以字节模式解析。 */
    public static ParseContext fromString(CharSequence text, Charset charset) {
        return new ParseContext(ParseSource.fromString(text, charset));
    }

    // ---------- 工厂：字符流 ----------

    /** 基于字符序列构造（字符模式，不经字节编码）。 */
    public static ParseContext fromChars(CharSequence text) {
        return new ParseContext(ParseSource.fromChars(text));
    }
}
