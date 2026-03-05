package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 字符源：基于 {@link CharSequence} 的按位置读取，不持有位置。
 */
public final class CharSource extends ParseTarget<CharSequence> {

    public CharSource(CharSequence sequence) {
        super(sequence);
    }

    /** 数据源长度（字符数）。 */
    public int getLength() {
        return getTarget().length();
    }

    /** 校验位置在 [0, getLength()] 内，否则抛 {@link IllegalArgumentException}。 */
    public void validatePosition(int position) {
        if (position < 0 || position > getLength()) {
            throw new IllegalArgumentException("position out of range: " + position + ", length: " + getLength());
        }
    }

    /** 从给定位置起剩余字符数。 */
    public int remainingChars(int position) {
        return getLength() - position;
    }

    /** 在指定位置读取一个字符。越界或不足时抛 {@link ReadException}。 */
    public char readChar(int position) throws ReadException {
        requireReadablePosition(position);
        return getTarget().charAt(position);
    }

    /** 从指定位置读取 n 个字符。不足时抛 {@link ReadException}。 */
    public char[] readChars(int position, int n) throws ReadException {
        requireCharsAvailable(position, n);
        CharSequence seq = getTarget();
        char[] arr = new char[n];
        if (seq instanceof String s) {
            s.getChars(position, position + n, arr, 0);
        } else {
            for (int i = 0; i < n; i++) {
                arr[i] = seq.charAt(position + i);
            }
        }
        return arr;
    }

    private void requireReadablePosition(int position) throws ReadException {
        if (position >= getTarget().length()) {
            ReadException ex = new ReadException("no remaining chars");
            ex.addContextValue("position", position);
            throw ex;
        }
        if (position < 0) {
            ReadException ex = new ReadException("index out of range: " + position + ", length: " + getTarget().length());
            ex.addContextValue("index", position);
            ex.addContextValue("length", getTarget().length());
            throw ex;
        }
    }

    private void requireCharsAvailable(int position, int n) throws ReadException {
        if (position < 0 || position + n > getTarget().length()) {
            ReadException ex = new ReadException("insufficient chars: need " + n + ", remaining " + remainingChars(position));
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("remaining", remainingChars(position));
            throw ex;
        }
    }

    public static CharSource fromChars(CharSequence text) {
        return new CharSource(text);
    }
}
