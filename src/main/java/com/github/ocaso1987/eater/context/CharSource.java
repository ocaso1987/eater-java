package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 字符源：基于 {@link CharSequence} 的按位置读取，不持有位置。
 */
public final class CharSource extends ParseSource<CharSequence> {

    public CharSource(CharSequence sequence) {
        super(sequence);
    }

    /** 数据源长度（字符数）。 */
    public int getLength() {
        return getSource().length();
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
        checkPosition(position, 1);
        return getSource().charAt(position);
    }

    /** 从指定位置读取 n 个字符。不足时抛 {@link ReadException}。 */
    public char[] readChars(int position, int n) throws ReadException {
        checkRange(position, n);
        CharSequence seq = getSource();
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

    private void checkPosition(int position, int required) throws ReadException {
        if (position >= getSource().length()) {
            ReadException ex = new ReadException("no remaining chars");
            ex.addContextValue("position", position);
            throw ex;
        }
        if (position < 0) {
            throw readExceptionIndex(position);
        }
    }

    private void checkRange(int position, int n) throws ReadException {
        if (position < 0 || position + n > getSource().length()) {
            ReadException ex = new ReadException("insufficient chars: need " + n + ", remaining " + remainingChars(position));
            ex.addContextValue("position", position);
            ex.addContextValue("required", n);
            ex.addContextValue("remaining", remainingChars(position));
            throw ex;
        }
    }

    private ReadException readExceptionIndex(int index) {
        ReadException ex = new ReadException("index out of range: " + index + ", length: " + getSource().length());
        ex.addContextValue("index", index);
        ex.addContextValue("length", getSource().length());
        return ex;
    }

    public static CharSource fromChars(CharSequence text) {
        return new CharSource(text);
    }
}
