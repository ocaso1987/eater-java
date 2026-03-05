package com.github.ocaso1987.eater.context;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 解析上下文：持有当前输入源（currentSource）、当前输出汇（currentSink）与解析状态（如 currentReadPosition/currentWritePosition、currentInputScope/currentOutputScope），不提供源的读取方式；
 * 具体读取由调用方通过 {@link #getSource()} 获取输入源后自行按类型调用 ByteSource/CharSource/ValueTarget 的方法；写出由 {@link #getSink()} 获取输出汇后按类型调用 ByteSink/CharSink。
 * {@link #getCurrentInputScope()} 表示对象源解析时的当前输入范围（含 parent 与 target），主要用于 {@link ValueTarget} 的层级解析。
 */
public final class ParseContext {

    private final ParseTarget<?> currentSource;
    private final ParseTarget<?> currentSink;
    private int currentReadPosition;
    private int currentWritePosition;
    private ParseScope currentInputScope;
    private ParseScope currentOutputScope;

    ParseContext(ParseTarget<?> currentSource, ParseTarget<?> currentSink) {
        this.currentSource = currentSource;
        this.currentSink = currentSink;
        this.currentReadPosition = 0;
        this.currentWritePosition = 0;
        this.currentInputScope = null;
        this.currentOutputScope = null;
    }

    ParseContext(ParseTarget<?> currentSource, ParseTarget<?> currentSink, ParseScope initialScope) {
        this.currentSource = currentSource;
        this.currentSink = currentSink;
        this.currentReadPosition = 0;
        this.currentWritePosition = 0;
        this.currentInputScope = initialScope;
        this.currentOutputScope = null;
    }

    /** 当前输入源（如 {@link ByteSource} / {@link CharSource} / {@link ValueTarget}）；未设置时为 null。 */
    public ParseTarget<?> getSource() {
        return currentSource;
    }

    /** 当前输出汇（如 {@link ByteSink} / {@link CharSink}）；未设置时为 null。 */
    public ParseTarget<?> getSink() {
        return currentSink;
    }

    /** 当前读取位置（仅字节源/字符源有意义）。 */
    public int currentReadPosition() {
        return currentReadPosition;
    }

    /**
     * 设置当前读取位置（用于回退或推进；仅字节源/字符源支持，对象源抛 {@link UnsupportedOperationException}）。
     */
    public void setCurrentReadPosition(int position) {
        if (currentSource instanceof ByteSource b) {
            b.validatePosition(position);
        } else if (currentSource instanceof CharSource c) {
            c.validatePosition(position);
        } else {
            throw new UnsupportedOperationException("setCurrentReadPosition not supported for " + (currentSource == null ? "null" : currentSource.getClass().getSimpleName()));
        }
        this.currentReadPosition = position;
    }

    /** 当前写入位置。 */
    public int currentWritePosition() {
        return currentWritePosition;
    }

    /** 设置当前写入位置。 */
    public void setCurrentWritePosition(int position) {
        this.currentWritePosition = position;
    }

    // ---------- 当前范围（主要用于 ValueTarget） ----------

    /** 当前输入解析范围（含 parent 与 target）；非对象源或未设置时为 null。 */
    public ParseScope getCurrentInputScope() {
        return currentInputScope;
    }

    /** 设置当前输入解析范围。 */
    public void setCurrentInputScope(ParseScope scope) {
        this.currentInputScope = scope;
    }

    /** 当前输出解析范围；未设置时为 null。 */
    public ParseScope getCurrentOutputScope() {
        return currentOutputScope;
    }

    /** 设置当前输出解析范围。 */
    public void setCurrentOutputScope(ParseScope scope) {
        this.currentOutputScope = scope;
    }

    /**
     * 进入输入子范围：以当前 input scope 为父、以 childTarget 为 target 创建新 scope 并设为当前输入范围。
     * 当前 input scope 为 null 时，创建根级 scope（parent 为 null）。
     *
     * @param childTarget 下级解析目标对象
     * @return 新的当前 input scope
     */
    public ParseScope enterInputScope(Object childTarget) {
        if (currentInputScope == null) {
            currentInputScope = new ParseScope(null, childTarget);
        } else {
            currentInputScope = currentInputScope.enter(childTarget);
        }
        return currentInputScope;
    }

    /** 退出当前输入范围，恢复到父 scope；若当前已是根或 null，则设为 null。 */
    public void exitInputScope() {
        currentInputScope = currentInputScope == null ? null : currentInputScope.exit();
    }

    /**
     * 进入输出子范围：以当前 output scope 为父、以 childTarget 为 target 创建新 scope 并设为当前输出范围。
     */
    public ParseScope enterOutputScope(Object childTarget) {
        if (currentOutputScope == null) {
            currentOutputScope = new ParseScope(null, childTarget);
        } else {
            currentOutputScope = currentOutputScope.enter(childTarget);
        }
        return currentOutputScope;
    }

    /** 退出当前输出范围，恢复到父 scope。 */
    public void exitOutputScope() {
        currentOutputScope = currentOutputScope == null ? null : currentOutputScope.exit();
    }

    // ---------- 工厂：字节流（仅输入） ----------

    public static ParseContext fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    public static ParseContext fromBytes(byte[] data, int offset, int length) {
        return new ParseContext(ByteSource.fromBytes(data, offset, length), null);
    }

    public static ParseContext fromByteBuffer(ByteBuffer buffer) {
        return new ParseContext(ByteSource.fromByteBuffer(buffer), null);
    }

    public static ParseContext fromEncodedBytes(CharSequence text, Charset charset) {
        return new ParseContext(ByteSource.fromString(
            Objects.requireNonNull(text, "text"),
            Objects.requireNonNull(charset, "charset")), null);
    }

    public static ParseContext fromEncodedBytesUtf8(CharSequence text) {
        return fromEncodedBytes(text, StandardCharsets.UTF_8);
    }

    // ---------- 工厂：字节流（仅输出） ----------

    /** 仅字节输出汇（currentSource 为 null）。 */
    public static ParseContext withByteSink(ByteSink sink) {
        return new ParseContext(null, Objects.requireNonNull(sink, "sink"));
    }

    // ---------- 工厂：字符流（仅输入） ----------

    public static ParseContext fromString(CharSequence text) {
        return new ParseContext(CharSource.fromChars(Objects.requireNonNull(text, "text")), null);
    }

    // ---------- 工厂：字符流（仅输出） ----------

    /** 仅字符输出汇（currentSource 为 null）。 */
    public static ParseContext withCharSink(CharSink sink) {
        return new ParseContext(null, Objects.requireNonNull(sink, "sink"));
    }

    // ---------- 工厂：对象源 ----------

    /** 从根对象创建上下文，初始 currentInputScope 为以 root 为 target 的根 scope。 */
    public static ParseContext fromObject(Object root) {
        ValueTarget src = new ValueTarget(Objects.requireNonNull(root, "root"));
        return new ParseContext(src, null, new ParseScope(null, root));
    }
}
