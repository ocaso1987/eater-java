package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.WriteException;

/**
 * 字符输出目标：基于 {@link Appendable} 持有输出流信息，支持按随机位置写入时要求底层为 {@link StringBuilder}，不持有位置。
 * 底层为 StringBuilder 时：空间不足会扩容，跳着写时中间空缺用占位符填位。
 */
public final class CharSink extends ParseTarget<Appendable> {

    /** 跳着写时空缺位置的占位字符。 */
    private static final char GAP_PLACEHOLDER = '\0';

    public CharSink(Appendable appendable) {
        super(appendable);
    }

    /** 在指定位置写入一个字符。仅当底层为 {@link StringBuilder} 时支持；空间不足会扩容，跳着写用占位符填位。 */
    public void writeChar(int position, char c) throws WriteException {
        StringBuilder sb = requireStringBuilder();
        ensureCapacityAndFillGap(sb, position, 1);
        sb.setCharAt(position, c);
    }

    /** 从指定位置写入 n 个字符。仅当底层为 {@link StringBuilder} 时支持；空间不足会扩容，跳着写用占位符填位。 */
    public void writeChars(int position, char[] chars) throws WriteException {
        writeChars(position, chars, 0, chars == null ? 0 : chars.length);
    }

    /** 从指定位置写入 chars[offset..offset+length)。仅当底层为 {@link StringBuilder} 时支持；空间不足会扩容，跳着写用占位符填位。 */
    public void writeChars(int position, char[] chars, int offset, int length) throws WriteException {
        if (chars == null || offset < 0 || length < 0 || offset + length > chars.length) {
            WriteException ex = new WriteException("invalid write: chars length " + (chars == null ? 0 : chars.length) + ", offset " + offset + ", length " + length);
            ex.addContextValue("position", position);
            throw ex;
        }
        StringBuilder sb = requireStringBuilder();
        ensureCapacityAndFillGap(sb, position, length);
        for (int i = 0; i < length; i++) {
            sb.setCharAt(position + i, chars[offset + i]);
        }
    }

    private StringBuilder requireStringBuilder() throws WriteException {
        Appendable target = getTarget();
        if (!(target instanceof StringBuilder sb)) {
            WriteException ex = new WriteException("CharSink random write requires StringBuilder");
            throw ex;
        }
        return sb;
    }

    /** 确保 [position, position+n) 可写：不足则扩容，若 position 大于当前长度则用占位符填位。 */
    private void ensureCapacityAndFillGap(StringBuilder sb, int position, int n) {
        int need = position + n;
        if (need <= sb.length()) {
            return;
        }
        sb.ensureCapacity(need);
        if (position > sb.length()) {
            for (int i = sb.length(); i < position; i++) {
                sb.append(GAP_PLACEHOLDER);
            }
        }
        if (need > sb.length()) {
            sb.setLength(need);
        }
    }

    public static CharSink fromAppendable(Appendable appendable) {
        return new CharSink(appendable);
    }

    public static CharSink fromStringBuilder(StringBuilder sb) {
        return new CharSink(sb);
    }
}
