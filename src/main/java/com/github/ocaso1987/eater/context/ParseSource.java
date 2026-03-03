package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.ReadException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 解析数据源：封装底层字节流（ByteBuffer）或字符流（CharSequence）的按索引读取，不持有位置。
 * 字节/字符模式互斥；在非当前模式下调用的读方法会抛 {@link UnsupportedOperationException}。
 * 越界或不足时抛 {@link ReadException}。
 */
public final class ParseSource {

    private final ByteBuffer byteBuffer;
    private final CharSequence charSequence;

    private ParseSource(ByteBuffer byteBuffer, CharSequence charSequence) {
        this.byteBuffer = byteBuffer;
        this.charSequence = charSequence;
    }

    /** 是否为字节模式。 */
    public boolean isByteMode() {
        return byteBuffer != null;
    }

    /** 是否为字符模式。 */
    public boolean isCharMode() {
        return charSequence != null;
    }

    /** 数据源长度（字节数或字符数，依当前模式）。 */
    public int getLength() {
        return byteBuffer != null ? byteBuffer.limit() : charSequence.length();
    }

    /** 校验位置在 [0, getLength()] 内，否则抛 {@link IllegalArgumentException}。 */
    public void validatePosition(int position) {
        if (position < 0 || position > getLength()) {
            throw new IllegalArgumentException("position out of range: " + position + ", length: " + getLength());
        }
    }

    /** 从给定位置起剩余字节数（仅字节模式有效）。 */
    public int remainingBytes(int position) {
        return byteBuffer != null ? getLength() - position : 0;
    }

    /** 从给定位置起剩余字符数（仅字符模式有效）。 */
    public int remainingChars(int position) {
        return charSequence != null ? getLength() - position : 0;
    }

    private void checkByteMode() {
        if (byteBuffer == null) {
            throw new UnsupportedOperationException("byte reading not supported in char mode");
        }
    }

    private void checkCharMode() {
        if (charSequence == null) {
            throw new UnsupportedOperationException("char reading not supported in byte mode");
        }
    }

    private void checkBytePosition(int position) throws ReadException {
        if (position >= byteBuffer.limit()) {
            ReadException ex = new ReadException("no remaining bytes");
            ex.addContextValue("position", position);
            throw ex;
        }
        if (position < 0) {
            ReadException ex = new ReadException("index out of range: " + position + ", length: " + byteBuffer.limit());
            ex.addContextValue("index", position);
            ex.addContextValue("length", byteBuffer.limit());
            throw ex;
        }
    }

    private void checkByteRange(int position, int n) throws ReadException {
        if (position < 0 || position + n > byteBuffer.limit()) {
            ReadException ex = new ReadException("insufficient bytes: need " + n + ", remaining " + remainingBytes(position));
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("remaining", remainingBytes(position));
            throw ex;
        }
    }

    private void checkCharPosition(int position) throws ReadException {
        if (position >= charSequence.length()) {
            ReadException ex = new ReadException("no remaining chars");
            ex.addContextValue("position", position);
            throw ex;
        }
        if (position < 0) {
            ReadException ex = new ReadException("index out of range: " + position + ", length: " + charSequence.length());
            ex.addContextValue("index", position);
            ex.addContextValue("length", charSequence.length());
            throw ex;
        }
    }

    private void checkCharRange(int position, int n) throws ReadException {
        if (position < 0 || position + n > charSequence.length()) {
            ReadException ex = new ReadException("insufficient chars: need " + n + ", remaining " + remainingChars(position));
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("remaining", remainingChars(position));
            throw ex;
        }
    }

    private void checkByteIndex(int index) throws ReadException {
        if (index < 0 || index >= byteBuffer.limit()) {
            ReadException ex = new ReadException("index out of range: " + index + ", length: " + byteBuffer.limit());
            ex.addContextValue("index", index);
            ex.addContextValue("length", byteBuffer.limit());
            throw ex;
        }
    }

    private void checkByteIndexRange(int index, int n) throws ReadException {
        if (index < 0 || index + n > byteBuffer.limit()) {
            ReadException ex = new ReadException("index or range out of bounds: index=" + index + ", n=" + n + ", length=" + byteBuffer.limit());
            ex.addContextValue("index", index);
            ex.addContextValue("n", n);
            ex.addContextValue("length", byteBuffer.limit());
            throw ex;
        }
    }

    private void checkCharIndex(int index) throws ReadException {
        if (index < 0 || index >= charSequence.length()) {
            ReadException ex = new ReadException("index out of range: " + index + ", length: " + charSequence.length());
            ex.addContextValue("index", index);
            ex.addContextValue("length", charSequence.length());
            throw ex;
        }
    }

    private void checkCharIndexRange(int index, int n) throws ReadException {
        if (index < 0 || index + n > charSequence.length()) {
            ReadException ex = new ReadException("index or range out of bounds: index=" + index + ", n=" + n + ", length=" + charSequence.length());
            ex.addContextValue("index", index);
            ex.addContextValue("n", n);
            ex.addContextValue("length", charSequence.length());
            throw ex;
        }
    }

    /** 在指定位置读取一个字节。越界或不足时抛 {@link ReadException}。 */
    public byte readByte(int position) throws ReadException {
        checkByteMode();
        checkBytePosition(position);
        return byteBuffer.get(position);
    }

    /** 从指定位置读取 n 个字节。不足时抛 {@link ReadException}。 */
    public byte[] readBytes(int position, int n) throws ReadException {
        checkByteMode();
        checkByteRange(position, n);
        byte[] arr = new byte[n];
        byteBuffer.get(position, arr, 0, n);
        return arr;
    }

    /** 在指定位置读取一个字符。越界或不足时抛 {@link ReadException}。 */
    public char readChar(int position) throws ReadException {
        checkCharMode();
        checkCharPosition(position);
        return charSequence.charAt(position);
    }

    /** 从指定位置读取 n 个字符。不足时抛 {@link ReadException}。 */
    public char[] readChars(int position, int n) throws ReadException {
        checkCharMode();
        checkCharRange(position, n);
        char[] arr = new char[n];
        if (charSequence instanceof String s) {
            s.getChars(position, position + n, arr, 0);
        } else {
            for (int i = 0; i < n; i++) {
                arr[i] = charSequence.charAt(position + i);
            }
        }
        return arr;
    }

    // ---------- 字节流（按索引） ----------

    public byte readByteAt(int index) throws ReadException {
        checkByteMode();
        checkByteIndex(index);
        return byteBuffer.get(index);
    }

    public byte[] readBytesAt(int index, int n) throws ReadException {
        checkByteMode();
        checkByteIndexRange(index, n);
        byte[] arr = new byte[n];
        byteBuffer.get(index, arr, 0, n);
        return arr;
    }

    // ---------- 字符流（按索引） ----------

    public char readCharAt(int index) throws ReadException {
        checkCharMode();
        checkCharIndex(index);
        return charSequence.charAt(index);
    }

    public char[] readCharsAt(int index, int n) throws ReadException {
        checkCharMode();
        checkCharIndexRange(index, n);
        char[] arr = new char[n];
        if (charSequence instanceof String s) {
            s.getChars(index, index + n, arr, 0);
        } else {
            for (int i = 0; i < n; i++) {
                arr[i] = charSequence.charAt(index + i);
            }
        }
        return arr;
    }

    // ---------- 工厂：字节流 ----------

    public static ParseSource fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    public static ParseSource fromBytes(byte[] data, int offset, int length) {
        ByteBuffer buf = ByteBuffer.wrap(data, offset, length).slice();
        return new ParseSource(buf, null);
    }

    public static ParseSource fromByteBuffer(ByteBuffer buffer) {
        return new ParseSource(buffer.slice(), null);
    }

    /** 按指定编码将字符串转为字节后以字节模式创建。 */
    public static ParseSource fromString(CharSequence text) {
        return fromString(text, StandardCharsets.UTF_8);
    }

    public static ParseSource fromString(CharSequence text, Charset charset) {
        ByteBuffer buffer = charset.encode(CharBuffer.wrap(text.toString()));
        return new ParseSource(buffer.slice(), null);
    }

    // ---------- 工厂：字符流 ----------

    public static ParseSource fromChars(CharSequence text) {
        return new ParseSource(null, text);
    }
}
